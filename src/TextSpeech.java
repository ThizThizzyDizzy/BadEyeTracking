import java.beans.PropertyVetoException;
import java.util.Locale;
import javax.speech.Central;
import javax.speech.synthesis.Synthesizer;
import javax.speech.synthesis.SynthesizerModeDesc;
public class TextSpeech{
    public boolean running = false;
    public Synthesizer synth;
    private float speed = 175f;
    private float vol = 1f;
    public TextSpeech(float speed, float vol){
        this.speed = speed;
        this.vol = vol;
        create();
    }
    public TextSpeech(){
        this(175f, 1f);
    }
    public void create(){
        if(running)return;
        try{
            System.setProperty("freetts.voices",
                    "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
            Central.registerEngineCentral("com.sun.speech.freetts.jsapi.FreeTTSEngineCentral");
            synth = Central.createSynthesizer(new SynthesizerModeDesc(Locale.US));
            synth.allocate();
            synth.getSynthesizerProperties().setSpeakingRate(speed);
            synth.getSynthesizerProperties().setVolume(vol);
            running = true;
        }catch(Exception ex){
            throw new RuntimeException(ex);
        }
            
    }
    public void setVolume(float vol) throws PropertyVetoException{
        this.vol = vol;
        synth.getSynthesizerProperties().setVolume(vol);
    }
    public void destroy(){
        if(!running)return;
        synth.cancelAll();
        try{
            synth.waitEngineState(Synthesizer.QUEUE_EMPTY);
            synth.deallocate();         
            running = false;
        }catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }
    public void say(String str){
        System.out.println("[TTS] "+str);
        if(!running){
            System.err.println("Cannot say "+str+"\nTTS system has not been initialized!");
            return;
        }
        try{
            synth.resume();
            synth.speakPlainText(str, null);         
        }catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }
    public void waitUntilSaid(){
        try{
            synth.waitEngineState(Synthesizer.QUEUE_EMPTY);
        }catch(InterruptedException|IllegalArgumentException ex){
            throw new RuntimeException(ex);
        }
    }
}