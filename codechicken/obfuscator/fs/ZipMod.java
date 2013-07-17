package codechicken.obfuscator.fs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipMod extends Mod
{
    private ZipInputStream input;
    private ZipOutputStream output;
    
    public ZipMod(ObfReadThread read, String name, InputStream in, boolean modify) throws IOException
    {
        super(read, name, modify);
        input = new ZipInputStream(in);
        if(modify)
        {
            File out = new File(read.outDir, name);
            if(out.exists())
                throw new RuntimeException("Duplicate output mod: "+name);
            
            out.getParentFile().mkdirs();
            out.createNewFile();
            
            output = new ZipOutputStream(new FileOutputStream(out));
        }
    }
    
    public void read(ModEntryReader reader) throws IOException
    {
        run.out().println("Reading mod: "+name);
        while(true)
        {
            ZipEntry e = input.getNextEntry();
            if(e == null)
                break;
            
            if(e.isDirectory())
                continue;
            
            reader.read(this, input, e.getName(), e.getSize());
        }
        
        input.close();
    }
    
    public void write(ModEntry entry) throws IOException
    {
        run.fine().println("Writing: "+entry.getName()+" to "+name);
        output.putNextEntry(new ZipEntry(entry.getName()));
        entry.write(output);
        output.closeEntry();
    }
    
    @Override
    public boolean writeAsync()
    {
        return false;
    }
    
    @Override
    public void close() throws IOException
    {
        output.close();
    }
}
