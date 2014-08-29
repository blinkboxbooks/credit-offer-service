require 'rubygems'
require 'bunny'
require 'sinatra'
require 'time'
require 'wrong'
require 'childprocess'
require 'httparty'
require 'sequel'

# Configure test properties
env = ENV['SERVER'] || 'local'
env_properties = YAML.load_file('features/support/config/environments.yml')[env]['services']
amqp_url = env_properties['amqp']
db_url = env_properties['db']

# Configure MQ channel
unless amqp_url
  puts 'No AMQP URL found'
  Process.exit(-1)
end

# Initialise MQ connection
$amqp_conn = Bunny.new(amqp_url)
$amqp_conn.start
$amqp_ch = $amqp_conn.create_channel

# Initialise db connection
$database = Sequel.connect(db_url)

# Starting http listener to listen to account-credit service requests
print 'Attempting to bring up http listener for account-credit service requests... '
begin
  http_listener = ChildProcess.build('ruby', File.join(File.dirname(__FILE__), 'mock_admin_account_credit_service.rb'), '-p', '9141')
  http_listener.start
  Wrong.eventually(timeout: 2) { HTTParty.get("http://localhost:9141/log").success? }
rescue
  puts 'FAILED'
  Process.exit(-1)
end
puts 'SUCCESS'

# Starting mock auth service
print 'Attempting to bring up mock auth service... '
begin
  mock_auth_service = ChildProcess.build('ruby', File.join(File.dirname(__FILE__), 'mock_auth_service.rb'))
  mock_auth_service.start
  Wrong.eventually(timeout: 2) { HTTParty.get("http://localhost:9393/admin/users/123").success? }
rescue
  puts 'FAILED'
  Process.exit(-1)
end
puts 'SUCCESS'

# Clean up.
at_exit do
  http_listener.stop if http_listener.alive?
  mock_auth_service.stop if mock_auth_service.alive?
  $amqp_conn.close
  $database.disconnect
end
