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

        spPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        spEditor = spPrefs.edit();

        synth = JSyn.createSynthesizer(new JSynAndroidAudioDevice());
        synth.add(lineOut = new LineOut());
        synth.add(whiteNoise = new WhiteNoise());
        synth.add(pinkNoise = new PinkNoise());
        synth.add(whiteNoiseLPF = new FilterLowPass());
        synth.add(pinkNoiseLPF = new FilterLowPass());
        synth.add(mixer = new PassThrough());

        whiteNoise.output.connect(whiteNoiseLPF.input);
        pinkNoise.output.connect(pinkNoiseLPF.input);

        whiteNoiseLPF.output.connect(mixer.input);
        pinkNoiseLPF.output.connect(mixer.input);

        mixer.output.connect(0, lineOut.input, 0);
        mixer.output.connect(0, lineOut.input, 1);

        whiteNoiseAmp = whiteNoise.amplitude;
        pinkNoiseAmp = pinkNoise.amplitude;

        whiteNoiseLPFFreq = whiteNoiseLPF.frequency;
        pinkNoiseLPFFreq = pinkNoiseLPF.frequency;

        whiteNoiseAmp.set((double)spPrefs.getFloat("wnAmp", (float)0.5));
        pinkNoiseAmp.set((double)spPrefs.getFloat("pnAmp", (float)0.5));

        whiteNoiseLPFFreq.set((double)spPrefs.getFloat("wnLPFFreq", (float)1000.0));
        pinkNoiseLPFFreq.set((double)spPrefs.getFloat("pnLPFFreq", (float)1000.0));

        final Button b = (Button) findViewById(R.id.actionButton);
        b.setOnClickListener(new View.OnClickListener() {
            boolean on = false;

            public void onClick(View v) {
                if (on) {
                    on = false;
                    stop();
                }
                else {
                    on = true;
                    start();
                }

                b.setText((on == true ? "Stop making noise" : "Start making noise"));
                Toast.makeText(MainActivity.this, "Whitenoise " + (on == true ? "on" : "off"), Toast.LENGTH_SHORT).show();
            }
        });

        final SeekBar sbWhiteNoiseAmp = (SeekBar) findViewById(R.id.wnAmpSeekBar);
        sbWhiteNoiseAmp.setProgress((int)(whiteNoiseAmp.get()*100.0));
        sbWhiteNoiseAmp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
        });

        final SeekBar sbPinkNoiseAmp = (SeekBar) findViewById(R.id.pnAmpSeekBar);
        sbPinkNoiseAmp.setProgress((int)(pinkNoiseAmp.get()*100.0));
        sbPinkNoiseAmp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
                pinkNoiseAmp.set(progress/100.0);
                spEditor.putFloat("pnAmp", ((float)progress/(float)100.0));
                spEditor.apply();
            }
        });

        final SeekBar sbWhiteNoiseTone = (SeekBar) findViewById(R.id.wnToneSeekBar);
        sbWhiteNoiseTone.setProgress((int)((whiteNoiseLPFFreq.get()/6000.0)*100.0));
        sbWhiteNoiseTone.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
                whiteNoiseLPFFreq.set(40 + 5960.0*((double)progress/100.0));
                spEditor.putFloat("wnLPFFreq", (float)6000.0*((float)progress/(float)100.0));
                spEditor.apply();
            }
        });

        final SeekBar sbPinkNoiseTone = (SeekBar) findViewById(R.id.pnToneSeekBar);
        sbPinkNoiseTone.setProgress((int)((pinkNoiseLPFFreq.get()/6000.0)*100.0));
        sbPinkNoiseTone.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
        });
    }

    protected void start() {
        stop();

        // Start synthesizer using default stereo output at 44100 Hz.
        synth.start();
        // Start the LineOut. It will pull data from the other units.
        lineOut.start();
    }

    protected void stop() {
        synth.stop();
    }
}
