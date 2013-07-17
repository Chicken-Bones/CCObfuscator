package codechicken.obfuscator.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DirMod extends FileMod
{
    private ZipOutputStream zipout;
    
    public DirMod(ObfReadThread read, File dir, boolean modify, boolean zip) throws IOException
    {
        super(read, dir, modify);
        if(modify() && zip)
        {
            File out = new File(read.outDir, name+".zip");
            if(out.exists())
                throw new RuntimeException("Duplicate output mod: "+name);
            
            out.getParentFile().mkdirs();
            out.createNewFile();
            
            zipout = new ZipOutputStream(new FileOutputStream(out));
        }
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
    
    @Override
    public void write(ModEntry entry) throws IOException
    {
        if(zipout != null)
        {
            run.fine().println("Writing: "+entry.getName()+" to "+name);
            zipout.putNextEntry(new ZipEntry(entry.getName()));
            entry.write(zipout);
            zipout.closeEntry();
        }
        else
        {
            super.write(entry);
        }
    }
    
    @Override
    public void close() throws IOException
    {
        if(zipout != null)
            zipout.close();
    }
}
