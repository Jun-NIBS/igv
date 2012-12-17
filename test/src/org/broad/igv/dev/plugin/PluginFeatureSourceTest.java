/*
 * Copyright (c) 2007-2012 The Broad Institute, Inc.
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 */

package org.broad.igv.dev.plugin;

import org.broad.igv.AbstractHeadlessTest;
import org.broad.igv.Globals;
import org.broad.igv.track.FeatureTrack;
import org.broad.igv.track.TrackLoader;
import org.broad.igv.util.ResourceLocator;
import org.broad.igv.util.RuntimeUtils;
import org.broad.igv.util.TestUtils;
import org.broad.tribble.Feature;
import org.junit.Assume;
import org.junit.Test;
import org.w3c.dom.Element;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * User: jacob
 * Date: 2012-Aug-10
 */
public class PluginFeatureSourceTest extends AbstractHeadlessTest {


    @Test
    public void testSimpleRuntimeIO() throws Exception{
        String[] fullCmd = null;
        String expLine = null;
        int expLines = -1;
        if(Globals.IS_MAC || Globals.IS_LINUX){
            fullCmd = new String[]{"/bin/bash", "-c", "whereis bash"};
            expLine = "/bin/bash";
            expLines = 1;
        }else if(Globals.IS_WINDOWS){
            fullCmd = new String[]{"cmd","/C","ver"};
            expLine = "Microsoft Windows";
            expLines = 2;
        }

        Assume.assumeNotNull(fullCmd);
        Process process = RuntimeUtils.startExternalProcess(fullCmd, null, null);
        InputStream is = process.getInputStream();
        BufferedReader bis = new BufferedReader(new InputStreamReader(is));

        String line;
        boolean foundLine = false;
        int count = 0;

        process.waitFor();
        while((line = bis.readLine()) != null){
            System.out.println(line);
            count++;
            foundLine |= line.contains(expLine);
        }
        assertTrue(foundLine);
        assertEquals(expLines, count);
    }


    @Test
    public void testGetFeaturesCat() throws Exception {

        Assume.assumeTrue(!Globals.IS_WINDOWS);

        PluginSpecReader reader = AbstractPluginTest.getCatReader();

        Element tool = reader.getTools().get(0);
        Element command = reader.getCommands(tool).get(0);
        List<Argument> argumentList = reader.getArguments(tool, command);


        LinkedHashMap<Argument, Object> arguments = new LinkedHashMap<Argument, Object>(argumentList.size());

        int argnum = 0;
        arguments.put(argumentList.get(argnum++), "");

        TrackLoader loader = new TrackLoader();
        String[] paths = new String[]{TestUtils.DATA_DIR + "bed/test.bed", TestUtils.DATA_DIR + "bed/testAlternateColor.bed"};
        for (String path : paths) {
            TestUtils.createIndex(path);
            FeatureTrack track = (FeatureTrack) loader.load(new ResourceLocator(path), genome).get(0);
            arguments.put(argumentList.get(argnum++), track);
        }


        List<String> cmd = Arrays.asList(reader.getToolPath(tool), command.getAttribute("cmd_arg"));
        PluginFeatureSource pluginSource = new PluginFeatureSource(cmd, arguments, reader.getParsingAttributes(tool, command), reader.getSpecPath());

        Iterator<Feature> featuresExp = ((FeatureTrack) arguments.get(argumentList.get(1))).getFeatures("chr2", 1, 30).iterator();
        Iterator<Feature> featuresAct = pluginSource.getFeatures("chr2", 1, 30);

        TestUtils.assertFeatureListsEqual(featuresExp, featuresAct);

        int start = 178707289 - 1;
        int end = 179714478;
        featuresExp = ((FeatureTrack) arguments.get(argumentList.get(2))).getFeatures("chr2", start, end).iterator();
        featuresAct = pluginSource.getFeatures("chr2", start, end);

        TestUtils.assertFeatureListsEqual(featuresExp, featuresAct);

    }
}
