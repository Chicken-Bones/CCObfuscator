package codechicken.obfuscator.fs;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import codechicken.lib.config.SimpleProperties;
import codechicken.obfuscator.ObfuscationRun;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import static java.util.Arrays.asList;

public class Main
{
    public static void main(String[] args)
    {     
        OptionParser parser = new OptionParser();
        parser.acceptsAll(asList("?", "help"), "Show the help");
        parser.acceptsAll(asList("d", "deobfuscate"), "Deobfuscate inputs");
        parser.acceptsAll(asList("r", "reobfuscate"), "Reobfuscate inputs");
        parser.acceptsAll(asList("i", "input"), "comma separated list of paths to class sources to be obfuscated (zips or directories)")
            .withRequiredArg().ofType(File.class).withValuesSeparatedBy(',');
        parser.acceptsAll(asList("l", "libs"), "comma separated list of dependant libraries for determining class heirachy")
            .withRequiredArg().ofType(File.class).withValuesSeparatedBy(',');
        parser.acceptsAll(asList("o", "out"), "Output Path")
            .withRequiredArg().ofType(File.class);
        parser.acceptsAll(asList("m", "mapping"), "MCP conf or Forge gradle unpacked directory")
            .withRequiredArg().ofType(File.class);
        parser.accepts("srg", "joined/packaged.srg file")
            .withRequiredArg().ofType(File.class);
        parser.accepts("fields", "fields.csv file")
            .withRequiredArg().ofType(File.class);
        parser.accepts("methods", "methods.csv file")
            .withRequiredArg().ofType(File.class);
        parser.acceptsAll(asList("c", "conf"), "Config file")
            .withRequiredArg().ofType(File.class);
        parser.acceptsAll(asList("noclean"), "Disable cleaning of the output dir");
        parser.acceptsAll(asList("q", "quiet"), "Disable error logging");
        parser.acceptsAll(asList("v", "verbose"), "Enable detailed logging");
        parser.acceptsAll(asList("zip"), "Set this to zip the contents of obfuscated directories");
        parser.acceptsAll(asList("srg"), "Obfuscate/Deobfuscate to srg names");
        parser.acceptsAll(asList("mcp"), "MCP dir, equal to -r " +
                "-input \"<mcp>/bin/minecraft\" " +
                "-libs \"<mcp>/lib,<mcp>/jars/libraries\" " +
                "-m \"<mcp>/conf\" " +
                "-o \"reobf/minecraft\"")
            .withOptionalArg().ofType(File.class);
        
        OptionSet options;
        try
        {
            options = parser.parse(args);
        }
        catch(OptionException ex)
        {
            System.err.println(ex.getLocalizedMessage());
            System.exit(-1);
            return;
        }
        
        try
        {
            main(parser, options);
        }
        catch(IOException e)
        {
            throw new RuntimeException(e);
        }
        catch (ParseException e)
        {
        }
    }

    private static void main(OptionParser parser, OptionSet options) throws IOException, ParseException
    {
        boolean obfuscate = true;
        File[] mods = null;
        File[] libs = null;
        File confDir = null;
        File outDir = null;
        File[] mappings = null;
        
        if(options.has("mcp"))
        {
            File mcp = options.hasArgument("mcp") ? (File) options.valueOf("mcp") : new File(".");
            obfuscate = true;
            mods = new File[]{new File(mcp, "bin/minecraft")};
            libs = new File[]{new File(mcp, "lib"), new File(mcp, "jars/libraries")};
            confDir = new File(mcp, "conf");
            outDir = new File(mcp, "reobf/minecraft");
        }
        else
        {
            if(options.has("deobfuscate") == options.has("reobfuscate"))
            {
                System.err.println("Either -r or -d required.");
                parser.printHelpOn(System.err);
                return;
            }
            
            require("input", parser, options);
            require("libs", parser, options);
            require("out", parser, options);
            
            if(!options.has("mapping")) {
                require("srg", parser, options);
                require("fields", parser, options);
                require("methods", parser, options);
            }
        }
        
        if(options.has("reobfuscate"))
            obfuscate = true;
        if(options.has("deobfuscate"))
            obfuscate = false;
        if(options.has("input"))
            mods = options.valuesOf("input").toArray(new File[0]);
        if(options.has("libs"))
            libs = options.valuesOf("libs").toArray(new File[0]);
        if(options.has("out"))
            outDir = (File) options.valueOf("out");
        if(options.has("mapping"))
            confDir = (File) options.valueOf("mapping");
        
        if(confDir != null)
            mappings = ObfuscationRun.parseConfDir(confDir);
        else
            mappings = new File[]{
                (File)options.valueOf("srg"),
                (File)options.valueOf("methods"),
                (File)options.valueOf("fields")};
        
        File confFile;
        if(options.has("conf"))
            confFile = (File) options.valueOf("conf");
        else
            confFile = new File("CCObfuscator.cfg");
        if(!confFile.exists())
        {
            confFile.getAbsoluteFile().getParentFile().mkdirs();
            confFile.createNewFile();
        }
        
        SimpleProperties p = new SimpleProperties(confFile);
        p.load();
        ObfuscationRun.fillDefaults(p.propertyMap);
        p.save();
        
        ObfuscationRun r = new ObfuscationRun(obfuscate, mappings, p.propertyMap);
        if(!options.has("noclean"))
            r.setClean();
        if(options.has("verbose"))
            r.setVerbose();
        if(options.has("quiet"))
            r.setQuiet();
        if(options.has("srg"))
            r.setSearge();
        
        new ObfReadThread(r, mods, libs, outDir, options.has("zip")).start();
    }

    private static void require(String string, OptionParser parser, OptionSet options) throws ParseException, IOException
    {
        if(!options.has(string))
        {
            System.err.println("--"+string+" required");
            parser.printHelpOn(System.err);
            throw new ParseException(null, 0);
        }
    }
}
