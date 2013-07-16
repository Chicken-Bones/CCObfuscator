package codechicken.obfuscator.fs;

import java.io.IOException;
import java.io.OutputStream;

public abstract class ModEntry
{
    public ModEntry(Mod mod)
    {
        this.mod = mod;
    }
    
    public final Mod mod;
    
    public abstract String getName();
    
    public abstract void write(OutputStream os) throws IOException;
}
