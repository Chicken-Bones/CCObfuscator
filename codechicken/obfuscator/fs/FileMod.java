package codechicken.obfuscator.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileMod extends Mod
{
    public static final String[] bannedFileNames = new String[]{"con", "prn", "aux", "nul"};
    public static final boolean isWindows = System.getProperty("os.name").startsWith("Windows");
    
    public static boolean isBanned(String name)
    {
        for(String b : bannedFileNames)
        {
            if(name.startsWith(b))
            {
                int i = name.indexOf('.');
                if(i < 0)
                    i = name.length();
                if(name.substring(0, i).equals(b))
                    return true;
            }
        }
        return false;
    }
    
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
        String fname = entry.getName();
        while(isBanned(fname))
            fname = '_'+fname;
            
        run.fine().println("Writing: "+fname+" to "+name);
        
        File out = new File(read.outDir, fname);
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
