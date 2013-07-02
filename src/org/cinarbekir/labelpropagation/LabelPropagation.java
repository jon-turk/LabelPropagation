/*
Copyright 2008-2012 Gephi
Authors : BEKİR ÇINAR <bkrcinar@gmail.com>
Website : http://www.gephi.org

This file is part of Gephi.

DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

Copyright 2012 Gephi Consortium. All rights reserved.

The contents of this file are subject to the terms of either the GNU
General Public License Version 3 only ("GPL") or the Common
Development and Distribution License("CDDL") (collectively, the
"License"). You may not use this file except in compliance with the
License. You can obtain a copy of the License at
http://gephi.org/about/legal/license-notice/
or /cddl-1.0.txt and /gpl-3.0.txt. See the License for the
specific language governing permissions and limitations under the
License.  When distributing the software, include this License Header
Notice in each file and include the License files at
/cddl-1.0.txt and /gpl-3.0.txt. If applicable, add the following below the
License Header, with the fields enclosed by brackets [] replaced by
your own identifying information:
"Portions Copyrighted [year] [name of copyright owner]"

If you wish your version of this file to be governed by only the CDDL
or only the GPL Version 3, indicate your decision by adding
"[Contributor] elects to include this software in this distribution
under the [CDDL or GPL Version 3] license." If you do not indicate a
single choice of license, a recipient has the option to distribute
your version of this file under either the CDDL, the GPL Version 3 or
to extend the choice of license to its licensees as provided above.
However, if you add GPL Version 3 code and therefore, elected the GPL
Version 3 license, then the option applies only if the new code is
made subject to such option by the copyright holder.

Contributor(s):

 Portions Copyrighted 2011 Gephi Consortium.
 */
package org.cinarbekir.labelpropagation;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeOrigin;
import org.gephi.data.attributes.api.AttributeRow;
import org.gephi.data.attributes.api.AttributeTable;
import org.gephi.data.attributes.api.AttributeType;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.UndirectedGraph;
import org.gephi.statistics.spi.Statistics;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;

public class LabelPropagation implements Statistics, LongTask {

    private String report = "";
    private boolean cancel = false;
    private ProgressTicket progressTicket;
    
    private boolean useDirection = false;
    private boolean useWeight = false;
    private boolean useDeterministe = false;
    private boolean useColor = false;
    private boolean useProgress = false;
    private boolean byNode = false;
    private boolean byIteration = false;
    /**
     * progression speed.
     */
    private int speedValue;
    private String plabel;
    private boolean control;
    /**
     * time passed while execution.
     */
    private long estimatedTime;
    private int communityNumber;
    
    public String labelColumn;

    /**
     * Beginning of traitement.
     * @param gm - graph model
     * @param am - attribute model
     */
    @Override
    public void execute(GraphModel gm, AttributeModel am) {      
        long startTime = System.currentTimeMillis();
        /**
         * setColor function accepts only float between 0 and 255 so colors numbers must be corresponded.
         */
        final float colorTranslate=255;
        Progress.start(progressTicket);
        Graph graph = gm.getGraph();
        AttributeTable nodeTable;
        graph.readLock();
        if (cancel) {
            graph.readUnlockAll();
            return;
        }
        nodeTable = am.getNodeTable();
          
        //controling if there is a label which has same name with ours, we change name of label if there is one.
        AttributeColumn inCol=createLabel(nodeTable);
      
        //initialisation of the labels
        initialisation(graph, inCol);
        
        //initilize colors
        List<Color> listOfColor=getColors(graph.getNodeCount()+1);

        if (useColor) {
            for (Node n : graph.getNodes()) {
                int ind = getNodeLabel(n);
                Color color = listOfColor.get(ind%359);
                n.getNodeData().setColor(
                        (float)color.getRed()/colorTranslate,(float) color.getBlue()/colorTranslate,
                        (float) color.getGreen()/colorTranslate);
            }
        }
           
        //list of nodes changing its label
        List<Node> changedNodeList=new ArrayList<Node>();
        control=true;
        while (control) {
            control=false;
            if (cancel) {
            graph.readUnlockAll();
            return;
            }
            changedNodeList.clear();
            // get the list of randomized nodes
            List<Node> nodeList = getRandomizedNodeLıst(graph);
          
            //changing labels 
            for (Node n : nodeList) {
                if (cancel) {
                    graph.readUnlockAll();
                    return;
                }
                // map getNeighborsLabelMap
                Map<Integer, Integer> hm = getNeighborsLabelMap(n, graph);

                if (!hm.isEmpty()) {
                    // getting majority labels
                    List<Integer> maxValueLabels = getMajorityLabels(hm);

                    //updateNodeLabel(n,maxValueLabels);
                    // updateNodeLabel(label)
                    if (!maxValueLabels.isEmpty()) {
                        boolean controlMax=false;
                        Integer nodesLabel=getNodeLabel(n);
                        for(Integer labelMax:maxValueLabels)
                        {
                            if (cancel) {
                                graph.readUnlockAll();
                                return;
                            }
                            if(labelMax.equals(nodesLabel))
                            {
                                controlMax=true;
                            }
                        }
                        if (!controlMax) {
                            Integer newLabel = maxValueLabels.get(0);
                            setNodeLabel(n, newLabel);
                            if(byNode)
                            {
                                int ind=getNodeLabel(n); 
                                Color color=listOfColor.get(ind%360);
                                n.getNodeData().setColor((float)color.getRed()/colorTranslate,
                                                        (float) color.getBlue()/colorTranslate,
                                                        (float) color.getGreen()/colorTranslate);
                                try {
                                    Thread.sleep(speedValue);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(LabelPropagation.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                            control=true;
                            changedNodeList.add(n);
                        }
                    }
                }
            }
            
            if(byIteration)
            {
                if (!changedNodeList.isEmpty()) {
                    for (Node n : changedNodeList) {
                        if (cancel) {
                            graph.readUnlockAll();
                            return;
                        }
                        int ind = getNodeLabel(n);
                        Color color = listOfColor.get(ind%360);
                        n.getNodeData().setColor((float)color.getRed()/colorTranslate,
                                                 (float) color.getBlue()/colorTranslate,
                                                 (float) color.getGreen()/colorTranslate);
                        try {
                            Thread.sleep(speedValue);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(LabelPropagation.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                
            }
        }
        
        if (useColor) {
            for (Node n : graph.getNodes()) {
                if (cancel) {
                    graph.readUnlockAll();
                    return;
                }
                int ind = getNodeLabel(n);
                Color color = listOfColor.get(ind%360);
                n.getNodeData().setColor(
                        (float)color.getRed()/colorTranslate,(float) color.getBlue()/colorTranslate,
                        (float) color.getGreen()/colorTranslate);
            }
        }
              
        //List of labels
        List<Integer> labelList = new ArrayList<Integer>();
        for (Node n : graph.getNodes()) {
            if (cancel) {
                graph.readUnlockAll();
                return;
            }
            if (!labelList.contains(getNodeLabel(n))) {
                labelList.add(getNodeLabel(n));
            }
        }
        
        //Number of communities
        communityNumber=labelList.size();
        
        //Number of time passed
        estimatedTime = System.currentTimeMillis() - startTime;
        
        //initialise report
        getReport();
        graph.readUnlock();
    }

    /**
     * Control cancel
     * @return true if cancel buton is pressed
     */
    @Override
    public boolean cancel() {
        cancel = true;
        return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket pt) {
        this.progressTicket = pt;
    }
/**
 * Initialise selection of direction
 * @param useDirection
 */
    public void setUseDirection(boolean useDirection) {
        this.useDirection = useDirection;
    }

    /**
     * Initialise selection of weight.
     * @param useWeight 
     */
    public void setUseWeight(boolean useWeight) {
        this.useWeight = useWeight;
    }
    
    /**
     * Initialise selection of determinism.
     * @param useDeterministe 
     */
    public void setUseDeterministe(boolean useDeterministe) {
        this.useDeterministe = useDeterministe;
    }

    /**
     * Initialise selection seeing of colors.
     * @param useColor 
     */
    public void setUseColor(boolean useColor) {
        this.useColor = useColor;
    }

    /**
     * Initialise selection of seeing progress.
     * @param useProgress 
     */
    public void setUseProgress(boolean useProgress) {
        this.useProgress = useProgress;
    }

    /**
     * Initialise selection of progression by node.
     * @param byNode 
     */
    public void setByNode(boolean byNode) {
        this.byNode = byNode;
    }

    /**
     * Initialise selection of progression by iteration.
     * @param byIteration 
     */
    public void setByIteration(boolean byIteration) {
        this.byIteration = byIteration;
    }

    /**
     * Initialise progression speed.
     * @param speedValue 
     */
    public void setSpeed(int speedValue) {
        this.speedValue = speedValue;
    }

    /**
     * It gives labels value.
     * @param n - node
     * @return 
     */
    public Integer getNodeLabel(Node n) {
        return Integer.parseInt(n.getNodeData().getAttributes().getValue(plabel).toString());
    }

    /**
     * It changes labels value.
     * @param n - node
     * @param newLabel - name of label.
     */
    public void setNodeLabel(Node n,Integer newLabel) {
        n.getNodeData().getAttributes().setValue(plabel, newLabel);
    }

    /**
     * It creates new column for propagation label.
     * @param nodeTable- Table which column will be created in.
     * @return new created column
     */
    public AttributeColumn createLabel(AttributeTable nodeTable)
    {
        //our label's name
        plabel = "plabel";
        labelColumn=plabel;
        String plabelt = "Propagation Label";
        
        //our new column
        AttributeColumn inCol;
        inCol = nodeTable.getColumn(plabel);
        
        int k = 0;
        while (inCol != null) {
            
            plabel = "plabel" + Integer.toString(k);
            plabelt = "Propagtion Label" + Integer.toString(k);
            labelColumn=plabel;
            inCol = nodeTable.getColumn(plabel);
            k++;
        }
        //when there isn't any, then we create a cloumn with that name
        if (inCol == null) {
            inCol = nodeTable.addColumn(plabel, plabelt, AttributeType.INT, AttributeOrigin.COMPUTED, 0);
        }
        return inCol;
    }

    /**
     * It initialises all node's specified column in given graph
     * @param graph 
     * @param inCol 
     */
    public void initialisation(Graph graph, AttributeColumn inCol) {

        int i = 1;

        //adding label to nodes
        for (Node n : graph.getNodes()) {
            if (cancel) {
                graph.readUnlockAll();
                return;
            }
            AttributeRow row = (AttributeRow) n.getNodeData().getAttributes();
            row.setValue(inCol, i);
            i++;
            Progress.progress(progressTicket);
        }
    }
    
    /**
     * It creates a list of color.
     * The size of list is 360 if graphs node number is equal or above 360.
     * Unless it depends on node number.
     * @param nodeNumber - number of node in the graph
     * @return color list
     */
    public List<Color> getColors(int nodeNumber) {
        List<Color> listOfColor=new ArrayList<Color>();
        int i,j;
        if(nodeNumber<360)
        {
            j=nodeNumber;
        }
        else{
            j=360;
        }
        Map<Float,Boolean> hControl=new HashMap<Float,Boolean>();
        for (i = 0; i < j; i++) {
            Random r = new Random();
            Float floatValue=r.nextFloat()*359;
            while(hControl.containsKey(floatValue))
            {
                floatValue=r.nextFloat()*359;
            }
            hControl.put(floatValue, true);    
            Color c = Color.getHSBColor(floatValue,1,1);
            listOfColor.add(c);
        }

        return listOfColor;
    }

    /**
     * It gives a list of nodes. 
     * Nodes are randomized in the list if determinism is checked in the beginning.
     * @param graph
     * @return list of nodes in given network
     */
    public List<Node> getRandomizedNodeLıst(Graph graph)
    {
        List<Node> nodeList = new ArrayList<Node>();
        for (Node n : graph.getNodes()) {
            if (cancel) {
                graph.readUnlockAll();
                break;
            }
            nodeList.add(n);
        }
        //we mix all nodes randomly
        if(!useDeterministe) {
            Collections.shuffle(nodeList);
        }
        
        return nodeList;
    }
    
    /**
     * It gives a list of majority labels.
     * @param hm - map which includes labels and their values
     * @return list of labels 
     */
    public List<Integer> getMajorityLabels(Map<Integer, Integer> hm) {
        Integer maxValue = Collections.max(hm.values());
        List<Integer> result = new ArrayList<Integer>();
        
        for (Integer j : hm.keySet()) {
            
            if (maxValue.equals(hm.get(j))) {
                result.add(j);
            }
        }
        Collections.shuffle(result);
        return result;
    }

    /**
     * It gives a map which includes neigbbors labels and their values of given npde.
     * @param n - node
     * @param graph
     * @return map
     */
    public Map<Integer, Integer> getNeighborsLabelMap(Node n, Graph graph) {
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        DirectedGraph dGraph = graph.getGraphModel().getDirectedGraph();
        UndirectedGraph uGraph = graph.getGraphModel().getUndirectedGraph();
       
        for (Node m : graph.getNeighbors(n)) {
            Progress.progress(progressTicket);
            Integer key = getNodeLabel(m);

            // init value for this label
            Integer value = 0;
            if (result.containsKey(key)) {
                value = result.get(key);
            }

            // update value for this label
            if (useWeight && useDirection) {
                Edge edge = graph.getEdge(m, n);
                if (edge != null&&edge.isDirected()) {
                    edge = dGraph.getEdge(m, n);
                }
                if (edge != null) {
                    int edgeWeight = (int) edge.getWeight();
                    value = value + edgeWeight;
                }
            } else if (useWeight && !useDirection) {
                Edge edge = uGraph.getEdge(m, n);
                int edgeWeight = (int) edge.getWeight();
                value = value + edgeWeight;
            } else if (!useWeight && useDirection) {
                Edge edge = graph.getEdge(m, n);
                if (edge != null&&edge.isDirected()) {
                    edge = dGraph.getEdge(m, n);
                }
                if (edge != null) {
                    value++;
                }
            } else if (!useWeight && !useDirection) {
                value++;
            }
            if (value > 0) {
                result.put(key, value);
            }

            if (cancel) {
               graph.readUnlockAll();
               break;
            }
        }
        return result;
    }
    
    /**
     * It gives a report of traitement.
     * @return a string
     */
    @Override
    public String getReport() {
        
        report = "<HTML> <BODY> <h1>Label Propagation Report </h1> "
                + "<hr>"
                + "<h2> Parameters: </h2>"
                + "Use directions:  "  + (useDirection ? "On" : "Off")+"<br>"
                + "Use weights:  " +(useWeight ? "On" : "Off")+ "<br>"                 
                + "Use colors:  " + (useColor ? "On" : "Off")+ "<br>" 
                + "Use progress:  " + (useProgress ? "On" : "Off")+ "<br>"
                + "<br> <h2> Results: </h2>"
                + "Number of Communities: " + communityNumber+"<br>"
                + "Time passed: "+estimatedTime+" ms"
                + "<br /><br />"
                + "<br /><br />" + "<h2> Algorithm: </h2>"
                + "Usha Nandini Raghavan,Réka Albert, and Soundar Kumara1 <i>Near linear time algorithm to detect community structures in large-scale networks</i>, in Journal of PHYSICAL REVIEW E 2007 (76), 036106 <br />"            
                + "<br /><br />" + "<h2> Author: </h2>"+"<br>"
                +"Bekir Çınar"
                + "</BODY> </HTML>";

        return report;
    }
}
