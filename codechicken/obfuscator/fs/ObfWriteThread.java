package codechicken.obfuscator.fs;

import codechicken.obfuscator.ObfuscationRun;

public class ObfWriteThread extends Thread
{
    public ObfReadThread read;
    public ObfuscationRun run;
    
    public ObfWriteThread(ObfReadThread read)
    {
        this.read = read;
        run = read.run;
        setName("Obfuscation Write");
    }
    
    @Override
    public void run()
    {
        while(true)
        {
            ModEntry e = read.getWriteEntry();
            if(e == null)
            {
                if(read.finishedReading())
                    break;
                continue;
            }
            
            try
            {
                e.mod.write(e);
            }
            catch(Exception ex)
            {
                run.err().println("Failed to write entry: "+e.getName()+" of mod "+e.mod.name);
                ex.printStackTrace(run.err());
                run.finish(true);
                return;
            }
        }
        
        for(Mod mod : read.modEntries)
        {
            try
            {
                mod.close();
            }
            catch(Exception e)
            {
                run.err().println("Failed to close mod: "+mod.name);
                e.printStackTrace(run.err());
                run.finish(true);
                return;
            }
        }
        
        run.finish(false);
    }
}
