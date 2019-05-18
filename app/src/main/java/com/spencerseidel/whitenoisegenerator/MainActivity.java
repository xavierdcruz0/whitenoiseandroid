package com.spencerseidel.whitenoisegenerator;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.hardware.SensorEventListener;

import com.jsyn.JSyn;
import com.jsyn.Synthesizer;
import com.jsyn.devices.android.JSynAndroidAudioDevice;
import com.jsyn.ports.UnitInputPort;
import com.jsyn.unitgen.LineOut;
import com.jsyn.unitgen.PassThrough;
import com.jsyn.unitgen.FilterLowPass;
import com.jsyn.unitgen.PinkNoise;
import com.jsyn.unitgen.WhiteNoise;

public class MainActivity extends AppCompatActivity {

    // GUI elements
    private Button m_actionButton;
    private SeekBar m_wnAmpSeekBar;
    private SeekBar m_pnAmpSeekBar;
    private SeekBar m_wnLPFFreqSeekBar;
    private SeekBar m_pnLPFFreqSeekBar;

    private SensorEventListener m_sensorEventListener;

    private AmplitudeHandler m_wnAmpHandler;
    private AmplitudeHandler m_pnAmpHandler;
    private FrequencyHandler m_wnFreqHandler;
    private FrequencyHandler m_pnFreqHandler;

    // Synth elements
    private Synthesizer m_synth;
    private LineOut m_lineOut;
    private WhiteNoise m_whiteNoise;
    private FilterLowPass m_whiteNoiseLPF;
    private PinkNoise m_pinkNoise;
    private FilterLowPass m_pinkNoiseLPF;
    private PassThrough m_mixer;

    private SharedPreferences spPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get default shared preferences
        spPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Grab our GUI elements by searching view
        m_actionButton = (Button)findViewById(R.id.actionButton);
        m_wnAmpSeekBar = (SeekBar)findViewById(R.id.wnAmpSeekBar);
        m_pnAmpSeekBar = (SeekBar)findViewById(R.id.pnAmpSeekBar);
        m_wnLPFFreqSeekBar = (SeekBar)findViewById(R.id.wnToneSeekBar);
        m_pnLPFFreqSeekBar = (SeekBar)findViewById(R.id.pnToneSeekBar);

        // Initialize our synth and add synth units
        m_synth = JSyn.createSynthesizer(new JSynAndroidAudioDevice());
        m_synth.add(m_lineOut = new LineOut());
        m_synth.add(m_whiteNoise = new WhiteNoise());
        m_synth.add(m_pinkNoise = new PinkNoise());
        m_synth.add(m_whiteNoiseLPF = new FilterLowPass());
        m_synth.add(m_pinkNoiseLPF = new FilterLowPass());
        m_synth.add(m_mixer = new PassThrough());

        // Create our synth unit chain by connecting outputs
        // with inputs:
        //
        // WN -> WNLPF ->-+
        //                |
        //                +--> passive passthrough mixer --> line out
        //                |
        // PN -> PMLPF ->-+
        //
        m_whiteNoise.output.connect(m_whiteNoiseLPF.input);
        m_pinkNoise.output.connect(m_pinkNoiseLPF.input);

        m_whiteNoiseLPF.output.connect(m_mixer.input);
        m_pinkNoiseLPF.output.connect(m_mixer.input);

        m_mixer.output.connect(0, m_lineOut.input, 0);
        m_mixer.output.connect(0, m_lineOut.input, 1);

        // Set listener for the action button (stop and start noise)
        m_actionButton.setOnClickListener(actionButtonListener);

        // Set up our handlers for amplitude Seekbar and UnitInputPorts
        m_wnAmpHandler = new AmplitudeHandler(m_whiteNoise.amplitude, m_wnAmpSeekBar, "wnAmp", spPrefs);
        m_wnAmpSeekBar.setOnSeekBarChangeListener(m_wnAmpHandler);
        m_pnAmpHandler = new AmplitudeHandler(m_pinkNoise.amplitude, m_pnAmpSeekBar, "pnAmp", spPrefs);
        m_pnAmpSeekBar.setOnSeekBarChangeListener(m_pnAmpHandler);

        // Set up our handlers for frequency Seekbar and UnitInputPorts
        m_wnFreqHandler = new FrequencyHandler(m_whiteNoiseLPF.frequency, m_wnLPFFreqSeekBar, "wnLPFFreq", spPrefs);
        m_wnLPFFreqSeekBar.setOnSeekBarChangeListener(m_wnFreqHandler);
        m_pnFreqHandler = new FrequencyHandler(m_pinkNoiseLPF.frequency, m_pnLPFFreqSeekBar, "pnLPFFreq", spPrefs);
        m_pnLPFFreqSeekBar.setOnSeekBarChangeListener(m_pnFreqHandler);

        // Set up synth according to saved prefs or defaults
        m_wnAmpHandler.setInitValue();
        m_pnAmpHandler.setInitValue();
        m_wnFreqHandler.setInitValue();
        m_pnFreqHandler.setInitValue();

        // Set up GUI elements to match the synth values
        m_wnAmpHandler.setSeekBarProgress();
        m_pnAmpHandler.setSeekBarProgress();
        m_wnFreqHandler.setSeekBarProgress();
        m_pnFreqHandler.setSeekBarProgress();
    }

    protected void start() {
        if (m_synth.isRunning()) {
            stop();
        }

        // Start synthesizer using default stereo output at 44100 Hz.
        m_synth.start();
        // Start the LineOut. It will pull data from the other units.
        m_lineOut.start();
    }

    protected void stop() {
        m_synth.stop();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Listener functions

    // We only have a single button, so we'll handle that with a simple listener function
    View.OnClickListener actionButtonListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (m_synth.isRunning()) {
                stop();
            }
            else {
                start();
            }

            m_actionButton.setText((m_synth.isRunning() ? "Stop making noise" : "Start making noise"));
            Toast.makeText(MainActivity.this, "Whitenoise " + (m_synth.isRunning() ? "on" : "off"), Toast.LENGTH_SHORT).show();
        }
    };

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Inner classes for handling amplitude and tone changes. These could probably use a common
    // parent class, but it's only two types, so . . .

    //  Amplitude/Seekbar handler
    class AmplitudeHandler implements SeekBar.OnSeekBarChangeListener {
        private int m_progress = 0;
        private String m_prefsString;
        private static final double m_MAX_SEEK_VALUE = 100.0;
        private static final double m_DEFAULT_AMP = 0.5;
        private SharedPreferences m_prefs;
        private UnitInputPort m_port;
        private SeekBar m_seekBar;

        AmplitudeHandler(UnitInputPort port, SeekBar seekBar, String prefsString, SharedPreferences prefs) {
            m_port = port;
            m_seekBar = seekBar;
            m_prefsString = prefsString;
            m_prefs = prefs;
        }

        void setSeekBarProgress() {
            m_seekBar.setProgress((int)(m_port.get()*m_MAX_SEEK_VALUE));
        }

        void setInitValue() {
            m_port.set((double)m_prefs.getFloat(m_prefsString, (float)m_DEFAULT_AMP));
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
            m_progress = progressValue;
            m_port.set((double)m_progress/m_MAX_SEEK_VALUE);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // NOP
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            SharedPreferences.Editor prefsEditor = m_prefs.edit();
            m_port.set((double)m_progress/m_MAX_SEEK_VALUE);
            prefsEditor.putFloat(m_prefsString, ((float)m_progress/(float)m_MAX_SEEK_VALUE));
            prefsEditor.apply();
        }
    }

    // Frequency/Seekbar Handler
    class FrequencyHandler implements SeekBar.OnSeekBarChangeListener {
        private int m_progress = 0;
        private String m_prefsString;
        private static final double m_MAX_SEEK_VALUE = 100.0;
        private static final double m_MAX_FREQ_VALUE = 5960.0;
        private static final double m_OFFSET_VALUE = 40.0;
        private static final double m_DEFAULT_FREQ = 1000.0;
        private SharedPreferences m_prefs;
        private UnitInputPort m_port;
        private SeekBar m_seekBar;

        FrequencyHandler(UnitInputPort port, SeekBar seekBar, String prefsString, SharedPreferences prefs) {
            m_port = port;
            m_seekBar = seekBar;
            m_prefsString = prefsString;
            m_prefs = prefs;
        }

        void setSeekBarProgress() {
            m_seekBar.setProgress((int)((m_port.get()/(m_MAX_FREQ_VALUE+m_OFFSET_VALUE))*100.0));
        }

        void setInitValue() {
            m_port.set((double)m_prefs.getFloat(m_prefsString, (float)m_DEFAULT_FREQ));
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
            m_progress = progressValue;
            m_port.set(m_OFFSET_VALUE + m_MAX_FREQ_VALUE*((double)m_progress/m_MAX_SEEK_VALUE));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // NOP
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            SharedPreferences.Editor prefsEditor = m_prefs.edit();

            m_port.set(m_OFFSET_VALUE + m_MAX_FREQ_VALUE*((double)m_progress/m_MAX_SEEK_VALUE));
            prefsEditor.putFloat(m_prefsString, (float)m_OFFSET_VALUE + (float)m_MAX_FREQ_VALUE*((float)m_progress/(float)m_MAX_SEEK_VALUE));
            prefsEditor.apply();
        }
    }
}
