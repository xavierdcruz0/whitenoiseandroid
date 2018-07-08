package com.spencerseidel.wng;

import java.awt.BorderLayout;

import javax.swing.JApplet;

import com.jsyn.JSyn;
import com.jsyn.Synthesizer;
import com.jsyn.instruments.DualOscillatorSynthVoice;
import com.jsyn.instruments.SubtractiveSynthVoice;
import com.jsyn.scope.AudioScope;
import com.jsyn.swing.JAppletFrame;
import com.jsyn.swing.SoundTweaker;
import com.jsyn.unitgen.LineOut;
import com.jsyn.unitgen.UnitSource;
import com.jsyn.unitgen.BrownNoise;
import com.jsyn.unitgen.WhiteNoise;
import com.jsyn.unitgen.RedNoise;
import com.jsyn.unitgen.PinkNoise;

import com.spencerseidel.wng.WindCircuit;

/**
 * Listen to a circuit while tweaking it knobs. Show output in a scope.
 *
 * @author Phil Burk (C) 2012 Mobileer Inc
 */
public class CircuitTester extends JApplet {
    private static final long serialVersionUID = -2704222221111608377L;
    private Synthesizer synth;
    private LineOut lineOut;
    private SoundTweaker tweaker;
    private UnitSource unitSource;
    private AudioScope scope;

    @Override
    public void init() {
        setLayout(new BorderLayout());

        synth = JSyn.createSynthesizer();
        synth.add(lineOut = new LineOut());

        unitSource = createUnitSource();
        synth.add(unitSource.getUnitGenerator());

        // Connect the source to both left and right speakers.
        unitSource.getOutput().connect(0, lineOut.input, 0);
        unitSource.getOutput().connect(0, lineOut.input, 1);

        tweaker = new SoundTweaker(synth, unitSource.getUnitGenerator().getClass().getName(),
                unitSource);
        add(tweaker, BorderLayout.CENTER);

        // Use a scope to see the output.
        scope = new AudioScope(synth);
        scope.addProbe(unitSource.getOutput());
        scope.setTriggerMode(AudioScope.TriggerMode.AUTO);
        scope.getView().setControlsVisible(false);
        add(BorderLayout.SOUTH, scope.getView());

        validate();
    }

    /**
     * Override this to test your own circuits.
     *
     * @return
     */
    public UnitSource createUnitSource() {
        //return new SampleHoldNoteBlaster();
        //return new com.syntona.exported.FMVoice();
        //return new DualOscillatorSynthVoice();
        //return new WindCircuit();
        //return new WhiteNoise();
        return new BrownNoise();
        //return new RedNoise();
        //return new PinkNoise();
    }

    @Override
    public void start() {
        // Start synthesizer using default stereo output at 44100 Hz.
        synth.start();
        // Start the LineOut. It will pull data from the other units.
        lineOut.start();

        scope.start();
    }

    @Override
    public void stop() {
        scope.stop();
        synth.stop();
    }

    /* Can be run as either an application or as an applet. */
    public static void main(String args[]) {
        CircuitTester applet = new CircuitTester();
        JAppletFrame frame = new JAppletFrame("JSyn Circuit Tester", applet);
        frame.setSize(600, 600);
        frame.setVisible(true);
        frame.test();
    }

}
