package codechicken.obfuscator.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import codechicken.obfuscator.ObfuscationRun;

public class ObfClassEntry extends ModEntry implements IRemappable
{
    public ClassNode cnode;
    public byte[] bytes;
    
    public ObfClassEntry(Mod mod, InputStream in) throws IOException
    {
        super(mod);
        
        cnode = new ClassNode();
        new ClassReader(in).accept(cnode, ClassReader.EXPAND_FRAMES);
    }
    
    @Override
    public String getName()
    {
        return mod.run.obfMapper.map(cnode.name) + ".class";
    }
    
    public void remap()
    {
        ObfuscationRun run = mod.run;
        run.fine().println("Remapping: "+cnode.name);

        ClassWriter cw = new ClassWriter(0);
        run.remap(cnode, cw);
        bytes = cw.toByteArray();
        
        mod.read.addWriteEntry(this);
    }
    
    @Override
    public void write(OutputStream os) throws IOException
    {
        os.write(bytes);
    }
}
