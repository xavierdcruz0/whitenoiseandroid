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
    private Button actionButton;
    private SeekBar wnAmpSeekBar;
    private SeekBar pnAmpSeekBar;
    private SeekBar wnLPFFreqSeekBar;
    private SeekBar pnLPFFreqSeekBar;

    // Synth elements
    private Synthesizer synth;
    private LineOut lineOut;
    private WhiteNoise whiteNoise;
    private UnitInputPort whiteNoiseAmp;
    private FilterLowPass whiteNoiseLPF;
    private UnitInputPort whiteNoiseLPFFreq;
    private PinkNoise pinkNoise;
    private UnitInputPort pinkNoiseAmp;
    private FilterLowPass pinkNoiseLPF;
    private UnitInputPort pinkNoiseLPFFreq;
    private PassThrough mixer;
    private SharedPreferences spPrefs;
    private SharedPreferences.Editor spEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Prefs
        spPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        spEditor = spPrefs.edit();

        // Initialize GUI elements
        actionButton = (Button)findViewById(R.id.actionButton);
        actionButton.setOnClickListener(actionButtonListener);

        wnAmpSeekBar = (SeekBar)findViewById(R.id.wnAmpSeekBar);
        wnAmpSeekBar.setOnSeekBarChangeListener(wnAmpSeekBarChangeListener);

        pnAmpSeekBar = (SeekBar)findViewById(R.id.pnAmpSeekBar);
        pnAmpSeekBar.setOnSeekBarChangeListener(pnAmpSeekBarChangeListener);

        wnLPFFreqSeekBar = (SeekBar)findViewById(R.id.wnToneSeekBar);
        wnLPFFreqSeekBar.setOnSeekBarChangeListener(wnToneSeekBarChangeListener);

        pnLPFFreqSeekBar = (SeekBar)findViewById(R.id.pnToneSeekBar);
        pnLPFFreqSeekBar.setOnSeekBarChangeListener(pnToneSeekBarChangeListener);

        // Set up synth
        synth = JSyn.createSynthesizer(new JSynAndroidAudioDevice());
        synth.add(lineOut = new LineOut());
        synth.add(whiteNoise = new WhiteNoise());
        synth.add(pinkNoise = new PinkNoise());
        synth.add(whiteNoiseLPF = new FilterLowPass());
        synth.add(pinkNoiseLPF = new FilterLowPass());
        synth.add(mixer = new PassThrough());

        // Connect units
        whiteNoise.output.connect(whiteNoiseLPF.input);
        pinkNoise.output.connect(pinkNoiseLPF.input);

        whiteNoiseLPF.output.connect(mixer.input);
        pinkNoiseLPF.output.connect(mixer.input);

        mixer.output.connect(0, lineOut.input, 0);
        mixer.output.connect(0, lineOut.input, 1);

        // For readability?
        whiteNoiseAmp = whiteNoise.amplitude;
        pinkNoiseAmp = pinkNoise.amplitude;
        whiteNoiseLPFFreq = whiteNoiseLPF.frequency;
        pinkNoiseLPFFreq = pinkNoiseLPF.frequency;

        // Set up synth according to saved prefs or defaults
        whiteNoiseAmp.set((double)spPrefs.getFloat("wnAmp", (float)0.5));
        pinkNoiseAmp.set((double)spPrefs.getFloat("pnAmp", (float)0.5));
        whiteNoiseLPFFreq.set((double)spPrefs.getFloat("wnLPFFreq", (float)1000.0));
        pinkNoiseLPFFreq.set((double)spPrefs.getFloat("pnLPFFreq", (float)1000.0));

        // Set up GUI elements to match the synth values
        wnAmpSeekBar.setProgress((int)(whiteNoiseAmp.get()*100.0));
        pnAmpSeekBar.setProgress((int)(pinkNoiseAmp.get()*100.0));
        wnLPFFreqSeekBar.setProgress((int)((whiteNoiseLPFFreq.get()/6000.0)*100.0));
        pnLPFFreqSeekBar.setProgress((int)((pinkNoiseLPFFreq.get()/6000.0)*100.0));
    }

    protected void start() {
        if (synth.isRunning()) {
            stop();
        }

        // Start synthesizer using default stereo output at 44100 Hz.
        synth.start();
        // Start the LineOut. It will pull data from the other units.
        lineOut.start();
    }

    protected void stop() {
        synth.stop();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Listeners
    View.OnClickListener actionButtonListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (synth.isRunning()) {
                stop();
            }
            else {
                start();
            }

            actionButton.setText((synth.isRunning() ? "Stop making noise" : "Start making noise"));
            Toast.makeText(MainActivity.this, "Whitenoise " + (synth.isRunning() ? "on" : "off"), Toast.LENGTH_SHORT).show();
        }
    };

    SeekBar.OnSeekBarChangeListener wnAmpSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        int progress = 0;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
            progress = progressValue;
            whiteNoiseAmp.set((double)progress/100.0);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // NOP
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            whiteNoiseAmp.set((double)progress/100.0);
            spEditor.putFloat("wnAmp", ((float)progress/(float)100.0));
            spEditor.apply();
        }
    };

    SeekBar.OnSeekBarChangeListener pnAmpSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        int progress = 0;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
            progress = progressValue;
            pinkNoiseAmp.set((double)progress/100.0);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // NOP
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            pinkNoiseAmp.set((double)progress/100.0);
            spEditor.putFloat("pnAmp", ((float)progress/(float)100.0));
            spEditor.apply();
        }
    };

    SeekBar.OnSeekBarChangeListener wnToneSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        int progress = 0;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
            progress = progressValue;
            whiteNoiseLPFFreq.set(40.0 + 5960.0*((double)progress/100.0));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // NOP
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            whiteNoiseLPFFreq.set(40.0 + 5960.0*((double)progress/100.0));
            spEditor.putFloat("wnLPFFreq", (float)40.0 + (float)5960.0*((float)progress/(float)100.0));
            spEditor.apply();
        }
    };

    SeekBar.OnSeekBarChangeListener pnToneSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        int progress = 0;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
            progress = progressValue;
            pinkNoiseLPFFreq.set(40.0 + 5960.0*((double)progress/100.0));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // NOP
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            pinkNoiseLPFFreq.set(40.0 + 5960.0*((double)progress/100.0));
            spEditor.putFloat("pnLPFFreq", (float)40.0 + (float)5960.0*((float)progress/(float)100.0));
            spEditor.apply();
        }
    };
}
