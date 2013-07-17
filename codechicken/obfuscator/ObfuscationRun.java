package codechicken.obfuscator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.tree.ClassNode;

import codechicken.lib.asm.ObfMapping;
import codechicken.obfuscator.ObfuscationMap.ObfuscationEntry;

import com.google.common.base.Function;

public class ObfuscationRun
{
    public final ObfuscationMap obf;
    public final ObfRemapper obfMapper;
    public final ConstantObfuscator cstMappper;
    
    public boolean obfuscate;
    public boolean srg;
    public boolean srg_cst;
    public File confDir;
    public Map<String, String> config;
    
    private PrintStream out = System.out;
    private PrintStream err = System.err;
    private PrintStream quietStream = new PrintStream(DummyOutputStream.instance);
    
    private IHeirachyEvaluator heirachyEvaluator;
    private HashSet<String> mappedClasses = new HashSet<String>();
    
    private boolean verbose;
    private boolean quiet;

    public boolean clean;
    private long startTime;
    
    public ObfuscationRun(boolean obfuscate, File confDir, Map<String, String> config)
    {
        this.obfuscate = obfuscate;
        this.confDir = confDir;
        this.config = config;
        
        obf = new ObfuscationMap(this);
        obfMapper = new ObfRemapper(this);
        cstMappper = new ConstantObfuscator(this);
    }
    
    public ObfuscationRun setClean()
    {
        clean = true;
        return this;
    }
    
    public ObfuscationRun setVerbose()
    {
        verbose = true;
        return this;
    }
    
    public ObfuscationRun setQuiet()
    {
        quiet = true;
        return this;
    }
    
    public ObfuscationRun setOut(PrintStream p)
    {
        out = p;
        return this;
    }
    
    public PrintStream out()
    {
        return quiet ? quietStream : out;
    }
    
    public PrintStream fine()
    {
        return verbose ? out : quietStream;
    }
    
    public ObfuscationRun setErr(PrintStream p)
    {
        err = p;
        return this;
    }
    
    public PrintStream err()
    {
        return quiet ? quietStream : err;
    }
    
    public ObfuscationRun setHeirachyEvaluator(IHeirachyEvaluator eval)
    {
        heirachyEvaluator = eval;
        return this;
    }

    public ObfuscationRun setSearge()
    {
        srg = true;
        return this;
    }

    public ObfuscationRun setSeargeConstants()
    {
        srg_cst = true;
        return this;
    }
    
    public void start()
    {
        startTime = System.currentTimeMillis();
    }
    
    public static Map<String, String> fillDefaults(Map<String, String> config)
    {
        if(!config.containsKey("excludedPackages"))
            config.put("excludedPackages", "java/;sun/;javax/;scala/;" +
                    "argo/;org/lwjgl/;org/objectweb/;org/bouncycastle/;com/google/");
        if(!config.containsKey("ignore"))
            config.put("ignore", ".");
        if(!config.containsKey("classConstantCalls"))
            config.put("classConstantCalls", 
                    "codechicken/core/asm/ObfMapping.<init>(Ljava/lang/String;)V," +
                    "codechicken/core/asm/ObfMapping.subclass(Ljava/lang/String;)Lcodechicken/core/asm/ObfuscationMappings$ObfMapping;," +
                    "codechicken/core/asm/ObfMapping.<init>(Lcodechicken/core/asm/ObfuscationMappings$ObfMapping;Ljava/lang/String;)V");
        if(!config.containsKey("descConstantCalls"))
            config.put("descConstantCalls", 
                    "codechicken/core/asm/ObfMapping.<init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V," +
                    "org/objectweb/asm/MethodVisitor.visitFieldInsn(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V," +
                    "org/objectweb/asm/tree/MethodNode.visitFieldInsn(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V," +
                    "org/objectweb/asm/MethodVisitor.visitMethodInsn(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V," +
                    "org/objectweb/asm/tree/MethodNode.visitMethodInsn(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V," +
                    "org/objectweb/asm/tree/MethodInsnNode.<init>(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V," +
                    "org/objectweb/asm/tree/FieldInsnNode.<init>(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
        
        return config;
    }

    public static void processLines(File file, Function<String, Void> function)
    {
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while((line = reader.readLine()) != null)
                function.apply(line);
            reader.close();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    public static void processFiles(File dir, Function<File, Void> function, boolean recursive)
    {
        for(File file : dir.listFiles())
            if(file.isDirectory() && recursive)
                processFiles(file, function, recursive);
            else
                function.apply(file);
    }
    
    public static void deleteDir(File directory, boolean remove)
    {
        if(!directory.exists())
        {
            if(!remove)directory.mkdirs();
            return;
        }
        for(File file : directory.listFiles())
        {
            if(file.isDirectory())
            {
                deleteDir(file, true);
            }
            else
            {
                if(!file.delete())
                    throw new RuntimeException("Delete Failed: "+file);
            }
        }
        if(remove)
        {
            if(!directory.delete())
                throw new RuntimeException("Delete Failed: "+directory);
        }
    }
    
    public long startTime()
    {
        return startTime;
    }
    
    public void parseMappings()
    {
        File srgs = new File(confDir, "packaged.srg");
        if(!srgs.exists())
            srgs = new File(confDir, "joined.srg");
        if(!srgs.exists())
            throw new RuntimeException("Could not find packaged.srg or joined.srg");
        File methods = new File(confDir, "methods.csv");
        if(!methods.exists())
            throw new RuntimeException("Could not find methods.csv");
        File fields = new File(confDir, "fields.csv");
        if(!fields.exists())
            throw new RuntimeException("Could not find fields.csv");
        
        parseSRGS(srgs);
        parseCSV(methods);
        parseCSV(fields);
    }
    
    public static String[] splitLast(String s, char c)
    {
        int i = s.lastIndexOf(c);
        return new String[]{s.substring(0, i), s.substring(i+1)};
    }
    
    public static String[] split4(String s, char c)
    {
        String[] split = new String[4];
        int i2 = s.indexOf(c);
        split[0] = s.substring(0, i2);
        int i = i2+1;
        i2 = s.indexOf(c, i);
        split[1] = s.substring(i, i2);
        i = i2+1;
        i2 = s.indexOf(c, i);
        split[2] = s.substring(i, i2);
        i = i2+1;
        i2 = s.indexOf(c, i);
        split[3] = s.substring(i);
        return split;
    }

    private void parseSRGS(File srgs)
    {
        out().println("Parsing "+srgs.getName());
        
        Function<String, Void> function = new Function<String, Void>()
        {
            @Override
            public Void apply(String line)
            {
                int hpos = line.indexOf('#');
                if(hpos > 0)
                    line = line.substring(0, hpos).trim();
                if(line.startsWith("CL: "))
                {
                    String[] params = splitLast(line.substring(4), ' ');
                    obf.addClass(params[0], params[1]);
                }
                else if(line.startsWith("FD: "))
                {
                    String[] params = splitLast(line.substring(4), ' ');
                    String[] p1 = splitLast(params[0], '/');
                    String[] p2 = splitLast(params[1], '/');
                    obf.addField(p1[0], p1[1], 
                            p2[0], p2[1]);
                    return null;
                }
                else if(line.startsWith("MD: "))
                {
                    String[] params = split4(line.substring(4), ' ');
                    String[] p1 = splitLast(params[0], '/');
                    String[] p2 = splitLast(params[2], '/');
                    obf.addMethod(p1[0], p1[1], params[1], 
                            p2[0], p2[1], params[3]);
                    return null;
                }
                return null;
            }
        };
        
        ObfuscationRun.processLines(srgs, function);
    }

    private void parseCSV(File csv)
    {
        out().println("Parsing "+csv.getName());
        
        Function<String, Void> function = new Function<String, Void>()
        {
            @Override
            public Void apply(String line)
            {
                if(line.startsWith("func_") || line.startsWith("field_"))
                {
                    int i = line.indexOf(',');
                    String srg = line.substring(0, i);
                    int i2 = i+1;
                    i = line.indexOf(',', i2);
                    String mcp = line.substring(i2, i);
                    
                    obf.addMcpName(srg, mcp);
                }
                return null;
            }
        };
        
        ObfuscationRun.processLines(csv, function);
    }
    
    private boolean isMapped(ObfuscationEntry desc)
    {
        return mappedClasses.contains(desc.srg.s_owner);
    }
    
    private ObfuscationEntry getOrCreateClassEntry(String name)
    {
        ObfuscationEntry e = obf.lookupObfClass(name);
        if(e == null)
            e = obf.lookupMcpClass(name);
        if(e == null)
            e = obf.addClass(name, name);//if the class isn't in obf or srg maps, it must be a custom mod class with no name change.
        return e;
    }

    public ObfuscationEntry buildSuperMap(String name)
    {
        ObfuscationEntry desc = getOrCreateClassEntry(name);
        if(isMapped(desc))
            return desc;

        mappedClasses.add(desc.srg.s_owner);
        
        if(!heirachyEvaluator.isLibClass(desc))
        {
            List<String> parents = heirachyEvaluator.getParents(desc);
            if(parents == null)
                err().println("Could not find class: "+desc.srg.s_owner);
            else
                for(String parent : parents)
                    inherit(desc, buildSuperMap(parent));
        }
        
        return desc;
    }

    public void inherit(ObfuscationEntry desc, ObfuscationEntry p_desc)
    {
        obf.inherit(desc.srg.s_owner, p_desc.srg.s_owner);
    }

    public void remap(ClassNode cnode, ClassVisitor cv)
    {
        cstMappper.transform(cnode);
        cnode.accept(new RemappingClassAdapter(cv, obfMapper));
    }

    public ObfMapping obfuscate(ObfuscationEntry map)
    {
        return srg ? map.srg : obfuscate ? map.obf : map.mcp;
    }

    public static List<String> getParents(ClassNode cnode)
    {
        List<String> parents = new LinkedList<String>();
        if(cnode.superName != null)
            parents.add(cnode.superName);
        
        for(String s_interface : cnode.interfaces)
            parents.add(s_interface);
        
        return parents;
    }
}
