package codechicken.obfuscator.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class DirMod extends FileMod
{
    public DirMod(ObfReadThread read, File dir, boolean modify)
    {
        super(read, dir, modify);
    }

    @Override
    public void read(ModEntryReader reader) throws IOException
    {
        run.out().println("Reading mod: "+name);
        read(file, "", reader);
    }
    
    private void read(File dir, String prefix, ModEntryReader reader) throws IOException
    {
        for(File file : dir.listFiles())
        {
            String name = prefix+(prefix.length() == 0 ? "" : "/")+file.getName();
            if(file.isDirectory())
                read(file, name, reader);
            else
            {
                FileInputStream in = new FileInputStream(file);
                reader.read(this, in, name, file.length());
                in.close();
            }
        }
    }
    
    @Override
    public boolean writeAsync()
    {
        return true;
    }
}
