package codechicken.obfuscator.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class ModFileEntry extends ModEntry
{
    public final DataBuffer buf;
    private final String name;
    
    public ModFileEntry(Mod mod, InputStream in, String name, long size) throws IOException
    {
        super(mod);
        this.name = name;
        buf = new DataBuffer(size);
        buf.read(in);
        buf.lock();
    }
    
    @Override
    public String getName()
    {
        return name;
    }
    
    @Override
    public void write(OutputStream out) throws IOException
    {
        buf.write(out);
    }
}
