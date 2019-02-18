package com.spencerseidel.wng;

import com.jsyn.ports.UnitInputPort;
import com.jsyn.ports.UnitOutputPort;
import com.jsyn.unitgen.Circuit;
import com.jsyn.unitgen.UnitSource;
import com.jsyn.unitgen.WhiteNoise;
import com.jsyn.unitgen.PinkNoise;
import com.jsyn.unitgen.PassThrough;
import com.jsyn.unitgen.FilterLowPass;

public class NoiseCircuit extends Circuit implements UnitSource {
    /* Declare units that will be part of the circuit. */
    WhiteNoise whiteNoise;
    PinkNoise pinkNoise;
    PassThrough passThrough;
    FilterLowPass lowPassFilterWhite;
    FilterLowPass lowPassFilterPink;

    /* Declare ports. */
    public UnitInputPort whiteNoiseAmp;
    public UnitInputPort pinkNoiseAmp;
    public UnitInputPort lowPassFilterWhiteFreq;
    public UnitInputPort lowPassFilterPinkFreq;
    public UnitOutputPort output;

    public NoiseCircuit() {
        /*
         * Create various unit generators and add them to circuit.
         */
        add(whiteNoise = new WhiteNoise());
        add(pinkNoise = new PinkNoise());
        add(passThrough = new PassThrough());
        add(lowPassFilterWhite = new FilterLowPass());
        add(lowPassFilterPink = new FilterLowPass());

        /* Make ports on internal units appear as ports on circuit. */
        /* Optionally give some circuit ports more meaningful names. */
        addPort(lowPassFilterWhiteFreq = lowPassFilterWhite.frequency, "LowPassFreqWhite");
        addPort(lowPassFilterPinkFreq = lowPassFilterPink.frequency, "LowPassFreqPink");
        addPort(whiteNoiseAmp = whiteNoise.amplitude, "WhiteNoiseAmp");
        addPort(pinkNoiseAmp = pinkNoise.amplitude, "PinkNoiseAmp");
        addPort(output = passThrough.output);

        whiteNoise.output.connect(lowPassFilterWhite.input);
        pinkNoise.output.connect(lowPassFilterPink.input);

        //whiteNoise.output.connect(passThrough.input);
        lowPassFilterWhite.output.connect(passThrough.input);
        //pinkNoise.output.connect(passThrough.input);
        lowPassFilterPink.output.connect(passThrough.input);

        /* Set ports to useful values and ranges. */
        whiteNoiseAmp.setup(0.0, 0.3, 1.0);
        pinkNoiseAmp.setup(0.0, 0.3, 1.0);
    }

    @Override
    public UnitOutputPort getOutput() {
        return output;
    }
}
