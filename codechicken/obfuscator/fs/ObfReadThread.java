package codechicken.obfuscator.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import codechicken.obfuscator.IHeirachyEvaluator;
import codechicken.obfuscator.ObfuscationRun;
import codechicken.obfuscator.ObfuscationMap.ObfuscationEntry;
import codechicken.obfuscator.fs.Mod.ModEntryReader;

public class ObfReadThread extends Thread implements ModEntryReader, IHeirachyEvaluator
{
    public ObfuscationRun run;
    
    public File[] mods;
    public File[] libs;
    public File outDir;
    
    private HashMap<String, ObfClassEntry> classMap = new HashMap<String, ObfClassEntry>();
    private LinkedList<IRemappable> classes = new LinkedList<IRemappable>();

    private String[] excludedPackages;
    private String[] ignoredPackages;
    private boolean zipDirs;
    
    private Queue<ModEntry> writeQueue = new LinkedList<ModEntry>();
    public List<Mod> modEntries = new LinkedList<Mod>();
    private boolean finishedReading = false;
    
    public ObfReadThread(ObfuscationRun run, File[] mods, File[] libs, File outDir, boolean zipDirs)
    {
        this.run = run;
        this.mods = mods;
        this.libs = libs;
        this.outDir = outDir;
        this.zipDirs = zipDirs;
        
        run.obf.setHeirachyEvaluator(this);
        excludedPackages = run.config.get("excludedPackages").split(";");
        ignoredPackages = run.config.get("ignore").split(";");
        setName("Obfuscation Read");
    }
    
    @Override
    public void run()
    {
        ObfWriteThread write = null;
        try
        {
            run.start();
            if(run.clean)
            {
                run.out().println("Cleaning: "+outDir.getName());
                ObfuscationRun.deleteDir(outDir, false);
            }
            
            run.parseMappings();
            
            write = new ObfWriteThread(this);
            write.start();
            
            readMods(libs, false);
            readMods(mods, true);
            
            obfuscate();
        }
        catch(Exception e)
        {
            e.printStackTrace(run.err());
            if(write == null)
                run.finish(true);
        }
        finishedReading = true;
    }
    
    private void readMods(File[] files, boolean modify)
    {
        for(File file : files)
        {
            try
            {
                if(file.isDirectory())
                {
                    new DirMod(this, file, modify, zipDirs).read(this);
                }
                else if(file.getName().endsWith(".jar") || file.getName().endsWith(".zip"))
                {
                    FileInputStream in = new FileInputStream(file);
                    new ZipMod(this, file.getName(), in, modify).read(this);
                    in.close();
                }
                else if(file.getName().endsWith(".class"))
                    new FileMod(this, file, modify).read(this);
                else
                    run.err().println("Unknown class source: "+file.getName());
                }
            catch(IOException e)
            {
                throw new RuntimeException("Failed to read mod: "+file.getName(), e);
            }
        }
    }

    @Override
    public void read(Mod mod, InputStream in, String e_name, long e_length) throws IOException
    {
        if(ignore(e_name))
            return;
        
        if((e_name.endsWith(".jar") || e_name.endsWith(".zip")) && mod.writeAsync())
        {
            new ZipMod(this, e_name, in, mod.modify()).read(this);
        }
        else if(e_name.endsWith(".class"))
        {
            if(exclude(e_name.replace('\\', '/')))
                return;
            
            ObfClassEntry oce = new ObfClassEntry(mod, in);
            ObfClassEntry existing = classMap.get(oce.cnode.name);
            if(existing != null && existing.mod.modify() == mod.modify())
            {
                run.err().println("Duplicate source found for "+oce.getName()+", "+existing.mod.name+" and "+mod.name);
            }
            else
            {
                if(existing == null || mod.modify())
                    classMap.put(oce.cnode.name, oce);
                
                if(mod.modify())
                    classes.add(oce);
            }
        }
        else if(mod.modify())
        {
            if(e_name.endsWith(".asm"))
                classes.add(new ASMFileEntry(mod, in, e_name));
            else
                addWriteEntry(new ModFileEntry(mod, in, e_name, e_length));
        }
    }
    
    public boolean exclude(String name)
    {
        for(String p : excludedPackages)
            if(name.startsWith(p))
                return true;
        
        return false;
    }
    
    public boolean ignore(String name)
    {
        for(String p : ignoredPackages)
            if(name.startsWith(p))
                return true;
        
        return false;
    }
    
    private void obfuscate()
    {
        run.out().println((run.obfDir.obfuscate ? "O" : "Deo")+"bfuscating classes");
        for(IRemappable e : classes)
            e.remap();
        
        run.out().println("Remapping finished.");
    }
    
    @Override
    public List<String> getParents(ObfuscationEntry desc)
    {
        ObfClassEntry e = classMap.get(desc.srg.s_owner);
        if(e == null)
            e = classMap.get(desc.obf.s_owner);
        if(e == null)
            return null;
        
        return ObfuscationRun.getParents(e.cnode);
    }
    
    @Override
    public boolean isLibClass(ObfuscationEntry desc)
    {
        return exclude(desc.srg.s_owner);
    }
    
    public void addWriteEntry(ModEntry e)
    {
        synchronized(writeQueue)
        {
            writeQueue.add(e);
        }
    }
    
    public ModEntry getWriteEntry()
    {
        synchronized(writeQueue)
        {
            return writeQueue.poll();
        }
    }
    
    public boolean finishedReading()
    {
        return finishedReading;
    }
}
