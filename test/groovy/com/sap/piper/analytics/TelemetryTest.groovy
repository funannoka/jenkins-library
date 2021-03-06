package com.sap.piper.analytics

import org.junit.Rule
import org.junit.Before
import org.junit.Test
import static org.junit.Assert.assertThat
import static org.junit.Assume.assumeThat
import org.junit.rules.ExpectedException
import org.junit.rules.RuleChain

import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.hasItem
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.not
import static org.hamcrest.Matchers.empty

import util.JenkinsLoggingRule
import util.JenkinsShellCallRule
import util.BasePiperTest
import util.Rules

class TelemetryTest extends BasePiperTest {
    private ExpectedException thrown = ExpectedException.none()
    private JenkinsLoggingRule jlr = new JenkinsLoggingRule(this)
    private JenkinsShellCallRule jscr = new JenkinsShellCallRule(this)

    @Rule
    public RuleChain rules = Rules
        .getCommonRules(this)
        .around(thrown)
        .around(jscr)
        .around(jlr)

    private parameters

    @Before
    void setup() {
        Telemetry.instance = null
        parameters = [:]
    }

    @Test
    void testCreateInstance() {
        Telemetry.instance = new Telemetry()
        // asserts
        assertThat(Telemetry.getInstance().listenerList, is(empty()))
    }

    @Test
    void testGetInstance() {
        // asserts
        assertThat(Telemetry.getInstance().listenerList, is(not(empty())))
    }

    @Test
    void testRegisterListenerAndNotify() {
        // prepare
        Map notificationPayload = [:]
        Telemetry.instance = new Telemetry()
        assumeThat(Telemetry.getInstance().listenerList, is(empty()))

        Telemetry.registerListener({ steps, payload ->
            notificationPayload = payload
        })
        // test
        Telemetry.notify(nullScript, [collectTelemetryData: true], [step: 'anyStep', anything: 'something'])
        // asserts
        assertThat(Telemetry.getInstance().listenerList, is(not(empty())))
        assertThat(notificationPayload, is([step: 'anyStep', anything: 'something']))
    }

    @Test
    void testNotifyWithOptOut() {
        // prepare
        Map notificationPayload = [:]
        Telemetry.instance = new Telemetry()
        assumeThat(Telemetry.getInstance().listenerList, is(empty()))
        Telemetry.registerListener({ steps, payload ->
            notificationPayload = payload
        })
        // test
        Telemetry.notify(nullScript, [collectTelemetryData: false], [step: 'anyStep', anything: 'something'])
        // asserts
        assertThat(Telemetry.getInstance().listenerList, is(not(empty())))
        assertThat(jlr.log, containsString("[anyStep] Sending telemetry data is disabled."))
        assertThat(notificationPayload.keySet(), is(empty()))
    }

    @Test
    void testNotifyWithOptOutWithEmptyConfig() {
        // prepare
        Map notificationPayload = [:]
        Telemetry.instance = new Telemetry()
        assumeThat(Telemetry.getInstance().listenerList, is(empty()))
        Telemetry.registerListener({ steps, payload ->
            notificationPayload = payload
        })
        // test
        Telemetry.notify(nullScript, [:], [step: 'anyStep', anything: 'something'])
        // asserts
        assertThat(Telemetry.getInstance().listenerList, is(not(empty())))
        assertThat(jlr.log, containsString("[anyStep] Sending telemetry data is disabled."))
        assertThat(notificationPayload.keySet(), is(empty()))
    }

    @Test
    void testNotifyWithOptOutWithoutConfig() {
        // prepare
        Map notificationPayload = [:]
        Telemetry.instance = new Telemetry()
        assumeThat(Telemetry.getInstance().listenerList, is(empty()))
        Telemetry.registerListener({ steps, payload ->
            notificationPayload = payload
        })
        // test
        Telemetry.notify(nullScript, null, [step: 'anyStep', anything: 'something'])
        // asserts
        assertThat(Telemetry.getInstance().listenerList, is(not(empty())))
        assertThat(jlr.log, containsString("[anyStep] Sending telemetry data is disabled."))
        assertThat(notificationPayload.keySet(), is(empty()))
    }

    @Test
    void testReportingToSWA() {
        // prepare
        assumeThat(Telemetry.getInstance().listenerList, is(not(empty())))
        // test
        Telemetry.notify(nullScript, [collectTelemetryData: true], [
            actionName: 'Piper Library OS',
            eventType: 'library-os',
            jobUrlSha1: '1234',
            buildUrlSha1: 'abcd',
            step: 'anyStep',
            stepParam1: 'something'
        ])
        // asserts
        assertThat(jscr.shell, hasItem(containsString('curl -G -v "https://webanalytics.cfapps.eu10.hana.ondemand.com/tracker/log"')))
        assertThat(jscr.shell, hasItem(containsString('--data-urlencode "action_name=Piper Library OS"')))
        assertThat(jscr.shell, hasItem(containsString('--data-urlencode "event_type=library-os"')))
        assertThat(jscr.shell, hasItem(containsString('--data-urlencode "custom3=anyStep"')))
        assertThat(jscr.shell, hasItem(containsString('--data-urlencode "custom4=1234"')))
        assertThat(jscr.shell, hasItem(containsString('--data-urlencode "custom5=abcd"')))
        assertThat(jscr.shell, hasItem(containsString('--data-urlencode "custom11=something"')))
    }
}
