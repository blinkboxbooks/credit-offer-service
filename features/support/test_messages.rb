
module KnowsAboutTestMessages
  def device_registration_template
    <<-EOS
<?xml version="1.0" encoding="UTF-8"?>
    <registered xmlns="http://schemas.blinkboxbooks.com/events/clients/v1"
xmlns:r="http://schemas.blinkboxbooks.com/messaging/routing/v1"
xmlns:v="http://schemas.blinkboxbooks.com/messaging/versioning"
r:originator="zuul"
v:version="1.0">

<userId>123</userId>
  <timestamp>2013-12-30T19:15:23Z</timestamp>

<client>
<id>19384</id>
    <name>My New Phone</name>
<brand>Hudl</brand>
    <model>[model]</model>
<os>android</os>
  </client>

</registered>
    EOS
  end


  def device_registration_hudl2_msg
    device_registration_template.gsub('[model]', 'Hudl 2')
  end

  def device_registration_hudl1_msg
    device_registration_template.gsub('[model]', 'Hudl')
  end

  def device_registration_again_msg
    <<-EOS
<?xml version="1.0" encoding="UTF-8"?>
    <registered xmlns="http://schemas.blinkboxbooks.com/events/clients/v1"
xmlns:r="http://schemas.blinkboxbooks.com/messaging/routing/v1"
xmlns:v="http://schemas.blinkboxbooks.com/messaging/versioning"
r:originator="zuul"
v:version="1.0">

<userId>123</userId>
  <timestamp>2013-12-30T20:15:23Z</timestamp>

<client>
<id>8888</id>
    <name>My New Phone</name>
<brand>Hudl</brand>
    <model>Hudl 2</model>
<os>android</os>
  </client>

</registered>
    EOS
  end

  def device_registration_invalid_format
    <<-EOS
    {"totally" : "invalid"}
    EOS
  end

  def device_registration_missing_fields #missing mandatory field model
    <<-EOS
<?xml version="1.0" encoding="UTF-8"?>
    <registered xmlns="http://schemas.blinkboxbooks.com/events/clients/v1"
xmlns:r="http://schemas.blinkboxbooks.com/messaging/routing/v1"
xmlns:v="http://schemas.blinkboxbooks.com/messaging/versioning"
r:originator="zuul"
v:version="1.0">

<userId>123</userId>
  <timestamp>2013-12-30T19:15:23Z</timestamp>

<client>
<id>19384</id>
    <name>My New Phone</name>
<brand>Hudl</brand>
<os>android</os>
  </client>

</registered>
    EOS
  end

  def credit_reporting_msg # events.users.v2.credited.schema.json
    {
        :$schema => "events.user.credited.v2",
        :timestamp => "2013-12-30T19:15:23Z",
        :user => {
            :id => 123,
            :username => "acceptance-test@blinkbox.com",
            :firstName => "acceptance",
            :lastName => "test",
        },
        :amount => 10.00,
        :currency => "GBP",
        :reason => "account_credit_hudl2"
    }
  end

  def send_mail_event_msg # actions.email.v2.send.schema.json
    {
        :$schema => "actions.email.send.v2",
        :timestamp => "2013-12-30T19:15:23Z",
        :to => {
            :username => "acceptance-test@blinkbox.com",
            :id => 123
        },
        :templateName => "account_credit_hudl2",
        :attributes => {
            :firstName => "acceptance",
            :lastName => "test"
        }
    }
  end

  def credit_request_body
    {:amount => '10.00', :currency => 'GBP', :reason => 'customer'}
  end
end

World(KnowsAboutTestMessages)
