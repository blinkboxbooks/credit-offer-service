require 'json'
require 'sequel'
require 'time'

DB = $database

Given(/^an existing user (has registered|registers) a Hudl(2|1)$/) do |tense, model|
  # set queue msg to device registration
  model == '2' ? @input_msg = device_registration_hudl2_msg : @input_msg = device_registration_hudl1_msg
  @expected_email_output = send_mail_event_msg
  @expected_reporting_output = credit_reporting_msg
  @expected_credit_output = credit_request_body

  if (/has/ =~ tense) # nothing more to do if we are just registering
    step ("the event is processed")

    sleep 1 # allowing time for credit-offer-service send messages and make requests
    purge_queues
    clear_http_listener_logs
  end
end

Given(/^the user registers another Hudl2$/) do
  #set queue msg - ensure device id is different to 1st msg
  @input_msg = device_registration_again_msg
end

# Given(/^an existing user has a Hudl2 registered to their account$/) do
#   #register a huddle2 - setup - clear queues
#   steps %Q{
#     Given an existing user registers a Hudl2
#     When the event is processed
#   }
#
#   sleep 1 # allowing time for credit-offer-service send messages and make requests
#   purge_queues
#   clear_http_listener_logs
# end

Given(/^a user triggers a Hudl2 registration event in an invalid format$/) do
  @input_msg = device_registration_invalid_format
end

Given(/^a user triggers a Hudl2 registration event with missing mandatory information$/) do
  @input_msg = device_registration_missing_fields
end

Given(/^the amount of credit given out has met the promotion limit$/) do
  msg = pop_message_from_queue('Exact.Target.Sender.Queue')
  # write some rows giving lots of credit in db
  DB[:promotions].insert(:user_id => 124,
                         :promotion_id => 'account_credit_hudl2',
                         :created_at => Date.today,
                         :credited_amount => 350000.00)
end

When(/^the event(?: for this)? is processed$/) do
  #cleaning logs from previous events
  clear_http_listener_logs

  # place msg on queue for promo service to consume
  publish_message_to_exchange('Events', @input_msg, 'events.clients.v1.registered')
end

Then(/^the user has £10 credited to their account$/) do
  sleep 2 # allowing time for credit-offer-service to make credit request

  # checking credit-offer-service makes credit request
  response = HTTParty.get('http://localhost:9141/log/requests')
  actual_credit_details = response.body
  expect(actual_credit_details).to eq (@expected_credit_output.to_json)

  # check that it is only called once
  credit_requests = HTTParty.get('http://localhost:9141/log/numRequests').body
  expect(credit_requests).to eq '1'

  #checking credit-offer-service records credit award in it's database
  results = DB[:promotions].where(user_id: 123)
  expect(results.count).to eq 1

  actual_row = results.all[0]
  expect(actual_row[:promotion_id]).to eq 'account_credit_hudl2'
  # is_time_just_now(actual_row[:created_at])
  expect(actual_row[:credited_amount]).to eq 10
end

Then(/^the user receives an e-mail confirming that their account has been deposited with £10 credit$/) do
  actual_email_event = JSON.parse(pop_message_from_queue('Exact.Target.Sender.Queue'))

  expect(actual_email_event['$schema']).to eq @expected_email_output[:$schema]
  expect(actual_email_event['to']['username']).to eq @expected_email_output[:to][:username]
  expect(actual_email_event['to']['id']).to eq @expected_email_output[:to][:id]
  expect(actual_email_event['templateName']).to eq @expected_email_output[:templateName]
  expect(actual_email_event['attributes']['first_name']).to eq @expected_email_output[:attributes][:first_name]
  expect(actual_email_event['attributes']['last_name']).to eq @expected_email_output[:attributes][:last_name]
end

Then(/^the user does not receive any (?:additional|promotional) credit$/) do
  credit_requests = HTTParty.get('http://localhost:9141/log/numRequests').body
  expect(credit_requests).to eq '0'
end

Then(/^the user does not receive an e-mail about promotional credit$/) do
  #check email queue is empty
  expect(is_queue_empty('Exact.Target.Sender.Queue')).to eq true
end

Then(/the event message is stored for later processing$/) do
  #check DLQ for original msg
  actual_event_msg = pop_message_from_queue('Credit.Offer.DeviceRegistration.DLQ')
  expect(actual_event_msg).to eq @input_msg
end

Then(/^the credit event was reported$/) do
  actual_reporting_event = JSON.parse(pop_message_from_queue('Reporting.UserCredit.Event'))

  expect(actual_reporting_event['$schema']).to eq @expected_reporting_output[:$schema]
  expect(actual_reporting_event['user']['id']).to eq @expected_reporting_output[:user][:id]
  expect(actual_reporting_event['user']['firstName']).to eq @expected_reporting_output[:user][:firstName]
  expect(actual_reporting_event['user']['lastName']).to eq @expected_reporting_output[:user][:lastName]
  expect(actual_reporting_event['amount']).to eq @expected_reporting_output[:amount]
  expect(actual_reporting_event['currency']).to eq @expected_reporting_output[:currency]
  expect(actual_reporting_event['reason']).to eq @expected_reporting_output[:reason]
end

Then(/^the credit event was not reported$/) do
  #check reporting queue is empty
  expect(is_queue_empty('Reporting.UserCredit.Event')).to eq true
end

def is_time_just_now(time_string)
  time = Time.parse(time_string.to_s)
  expect(Time.now.utc - time).to be <= 3
end