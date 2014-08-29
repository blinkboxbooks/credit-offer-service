require 'sinatra'

# This specifies a mock Auth service

@@ccp_feature_on = false

configure do
  set :port, 9393
  set :host, 'localhost'
end

before do
  content_type 'application/json'
end

post '/oauth2/token' do
  "{
      \"access_token\": \"eyJraWQiOiJibGlua2JveC9wbGF0L2VuYy9yc2EvMSIsImN0eSI6IkpXVCIsImVuYyI6IkExMjhHQ00iLCJhbGciOiJSU0EtT0FFUCJ9.MRvf2DYJ4TQoaKwpK2ZOCd5mmWTgcyl_h0ZLsfE4hr5g_tjReebO5lUzMYI0ffyACqOiXJLeqezg7VwGqZJwiS1VAMiNxkgp7LDnMo9N5JHPIVxllQR-9Jd-kGiuojKdAJrh7tvSDytnv7NSmHYTzVNkk_xUnCrt8rHzykZbnjsN5GZXOcL1RJp8cN5MOhYEaXiNBUGPj92iQ8k61TFrLGqJlAhoRTf1IupDqwrQTgUgJy5aw8cbfmHM4I_BLLOM_sx0K0VjLEITy-8bChWJxELZt0PGlISTHxTEavkSvfLbW_DhwVKEW3nsI2YwxkekWe9qxUz8JhkHpqmb0yX0iw.plexz6SO9_1RbdNC.j4w9cxQnk6bBzyJZNZBWwvBwqobmYZWzwcq1dbodYOkADWDuVZ6UrVmJjTMggUc236QeKzI-VgesyniKQGdMbbIQS5lcFE_dkTKF5Fr4BV3gXTI8k38YP3UqrorzQmLaRp5R2Xt-u07ulZKmfyX20smAlN1VOPHD0oYQey6NMdm8iHfYzZlVtikgG8RT1meOfCYABfZF0wyq-LHDAr2ogM6HJAUVzZaDBAmZLIqiruc7iLAGfPY88q-2msnDGZPwjJbXKotvtuBLAAGb-O8cUlZXXt1E2l-i-AXvQps5b_-Gy-FgDhjxBlZKGNcm-uiebcA.TWW5zutKJ3fDkCJmRNr7mQ\",
      \"token_type\": \"bearer\",
      \"expires_in\": 1800,
      \"refresh_token\": \"of8VY_DlPIIA_w2aBh64d38h8FjqO27BVkbbp38K2zc\",
      \"user_id\": \"urn:blinkbox:zuul:user:470\",
      \"user_uri\": \"/users/470\",
      \"user_username\": \"muld4@mail.ru\",
      \"user_first_name\": \"Muld-Live\",
      \"user_last_name\": \"testblinknow\"
  }"
end

get '/admin/users/*' do
  "{
      \"user_id\": \"urn:blinkbox:zuul:user:814\",
      \"user_uri\": \"https://auth.blinkboxbooks.com/users/123\",
      \"user_username\": \"acceptance-test@blinkbox.com\",
      \"user_first_name\": \"acceptance\",
      \"user_last_name\": \"test\",
      \"user_allow_marketing_communications\": true
  }"
end
