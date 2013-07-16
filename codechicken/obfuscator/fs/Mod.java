package codechicken.obfuscator.fs;

import java.io.IOException;
import java.io.InputStream;

import codechicken.obfuscator.ObfuscationRun;

public abstract class Mod
{
    public static interface ModEntryReader
    {
        public void read(Mod mod, InputStream in, String e_name, long e_length) throws IOException;
    }
    
    public String name;
    private boolean modify;

    public final ObfuscationRun run;
    public final ObfReadThread read;
    
    public Mod(ObfReadThread read, String name, boolean modify)
    {
        this.read = read;
        run = read.run;
        this.name = name;
        setModify(modify);
    }
    
    public boolean modify()
    {
        return modify;
    }
    
    public void setModify(boolean b)
    {
        if(b == modify)
            return;
        
        modify = b;
        if(modify)
            read.modEntries.add(this);
        else
            read.modEntries.remove(this);
    }

    public abstract void read(ModEntryReader reader) throws IOException;

    public abstract void write(ModEntry entry) throws IOException;
    
    public abstract boolean writeAsync();
    
    public void close() throws IOException
    {
    }
}
