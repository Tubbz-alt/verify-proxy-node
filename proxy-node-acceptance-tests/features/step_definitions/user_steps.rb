require 'yaml'
require 'uri'
require 'securerandom'

def country_stub_connector_url(country)
  case country
  when 'Netherlands'
    'https://demo-portal.minez.nl/demoportal/etoegang'
  else
    raise ArgumentError.new("Invalid country name: #{country}")
  end
end

Given("the Proxy Node is sent an LOA {string} request from the Stub Connector") do |load_type|
  loa_url = case load_type
    when "Low"
      "/RequestLow"
    when "Substantial"
      "/RequestSubstantial"
    when "High"
      "/RequestHigh"
    else
      "/BadRequest"
  end
  visit(ENV.fetch('STUB_CONNECTOR_URL') + loa_url)
end

Given("the proxy node is sent a transient PID request") do
  visit(ENV.fetch('STUB_CONNECTOR_URL') + "/RequestTransientPid")
end

And('they progress through Verify') do
  assert_text('Sign in with GOV.UK Verify')
  choose('start_form_selection_false', allow_label_click: true)
  click_button('Continue')
  find('button', :text => 'Stub Idp Demo One').click
end

Given(/^the Stub Connector supplies an authentication request with (.*)$/) do |issue|
  scenario_path_map = {
      "a missing signature": "/MissingSignature",
      "an invalid signature": "/InvalidSignature"
  }
  visit(ENV.fetch('STUB_CONNECTOR_URL') + scenario_path_map[issue.to_sym])
end

Given('they login to Stub IDP') do
  fill_in('username', with: ENV.fetch('STUB_IDP_USER'))
  fill_in('password', with: 'bar')
  click_on('SignIn')
  click_on('I Agree')
end

Given('they login to Stub IDP with error event {string}') do |error_button_text|
  fill_in('username', with: ENV.fetch('STUB_IDP_USER'))
  fill_in('password', with: 'bar')
  click_on(error_button_text)
end

Given("the user accesses a invalid page") do
  visit(ENV.fetch('PROXY_NODE_URL') + '/asdfasdfasfsaf')
end

Given("the user accesses the Gateway response URL directly") do
  visit(ENV.fetch('PROXY_NODE_URL') + '/SAML2/SSO/Response/POST')
end

Given("the user visits the {string} Stub Connector Node page") do |country|
  visit(country_stub_connector_url(country))
end

And('they navigate {string} journey to verify with UK identity') do |country|
  case country
  when 'Netherlands'
    navigate_netherlands_journey_to_uk
  else
    raise ArgumentError.new("Invalid country name: #{country}")
  end
end

Then('they should arrive at the Verify Hub start page') do
  assert_text('Sign in with GOV.UK Verify')
end

Then('they should arrive at the Stub Connector success page') do
  assert_text('Response successfully received')
  assert_text('Jack Cornelius')
  assert_text('Bauer')
  assert_text('1984-02-29')
end

And('they should have a transient PID') do
  assert_text('GB/EU/_tr_')
end

Then("the user should be presented with a Hub error page indicating IDP could not sign them in") do
  assert_text('Stub Idp Demo One couldn’t sign you in')
  assert_text('You may have selected the wrong company. Check your emails and text messages for confirmation of who verified you.')
end

Then("the user should be presented with an error page") do
  assert_text('Sorry, something went wrong')
  assert_text('This may be because your session timed out or there was a system error.')
end

def navigate_netherlands_journey_to_uk
  assert_text('Kies hoe u wilt inloggen')
  click_link('English')
  assert_text('Choose how to log in')
  select "EU Login", :from => "authnServiceId"
  click_button('Continue')
  assert_text('Which country is your ID from?')
  find('#country-GB').click
  click_button('Continue')
end
