require 'sinatra'

# This specifies a mock Admin service

@@request_bodies = []

configure do
  set :host, 'localhost'
end

before do
  content_type 'text/plain'
end

delete '/log/clear' do
  @@request_bodies.clear
  'cleared log'
end

get '/admin/users/*/credit' do
  response.headers['content-type'] = 'application/vnd.blinkboxbooks.data.v1+json'

  "{\"type\":\"urn:blinkboxbooks:schema:list\",
  \"items\":[{\"type\":\"urn:blinkboxbooks:schema:admin:credit\",\"amount\":\"1092.65\",\"currency\":\"GBP\"},
  {\"type\":\"urn:blinkboxbooks:schema:admin:credit\",\"amount\":\"30.19\",\"currency\":\"EUR\"}]}"
end

get '/log/requests' do
  unless params['requestNo']
    p @@request_bodies.last
    return @@request_bodies.last
  end
  @@request_bodies[params['requestNo'].to_i]
end

get '/log/numRequests' do
  @@request_bodies.count.to_s
end

get '/*' do
  log_request
end

post '/admin/users/*/credit' do
  log_request

  response.headers['content-type'] = 'application/vnd.blinkboxbooks.data.v1+json'
  "{
      \"type\": \"urn:blinkboxbooks:schema:admin:credit\",
      \"amount\": \"10.00\",
      \"currency\": \"GBP\"
  }"
end

def log_request
  @@request_bodies << URI.unescape(request.body.read) #<< request.body.read #
  print_request_bodies
  'Request logged'
end

def print_request_bodies
  @@request_bodies.each { |body| p 'logged this: ' + body }
end
