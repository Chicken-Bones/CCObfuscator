package codechicken.obfuscator.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileMod extends Mod
{
    public final File file;
    
    public FileMod(ObfReadThread read, File file, boolean modify)
    {
        super(read, file.getName(), modify);
        this.file = file;
    }

    @Override
    public void read(ModEntryReader reader) throws IOException
    {
        run.out().println("Reading mod: "+name);
        FileInputStream in = new FileInputStream(file);
        reader.read(this, in, file.getName(), file.length());
        in.close();
    }

    @Override
    public void write(ModEntry entry) throws IOException
    {
        run.fine().println("Writing: "+entry.getName()+" to "+name);
        
        File out = new File(read.outDir, entry.getName());
        if(out.exists())
            throw new RuntimeException("Duplicate output mod: "+out.getName());
        
        out.getParentFile().mkdirs();
        out.createNewFile();
        
        FileOutputStream fout = new FileOutputStream(out);
        entry.write(fout);
        fout.close();
    }

    @Override
    public boolean writeAsync()
    {
        return false;
    }
}
