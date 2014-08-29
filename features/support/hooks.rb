Before do
  initialize_queues
  purge_queues
  clear_http_listener_logs
  DB[:promotions].where(:user_id => 123).delete
  DB[:promotions].where(:user_id => 124).delete
end

def initialize_queues
  create_exchange('Agora', 'headers')
  create_exchange('Events', 'topic')

  create_queue('Reporting.UserCredit.Event')
  create_queue('Exact.Target.Sender.Queue')
  create_queue('Credit.Offer.DeviceRegistration.DLQ', true)
  create_queue('Credit.Offer.DeviceRegistration', true)

  queue('Exact.Target.Sender.Queue').bind('Agora', :arguments => {:content_type => 'application/vnd.blinkbox.books.events.email.send.v2+json'})
  queue('Reporting.UserCredit.Event').bind('Agora', :arguments => {:content_type => 'application/vnd.blinkbox.books.events.user.credited.v2+json'})
end

def clear_http_listener_logs
  HTTParty.delete('http://localhost:9141/log/clear')
end