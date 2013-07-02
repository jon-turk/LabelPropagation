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

import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Hashtable;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.EdgeIterator;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.openide.util.Lookup;

class LabelPropagationPanel extends JPanel{
    
    private boolean useDirections=false;
    private JCheckBox useDirection;
    private JCheckBox useWeight;
    private JCheckBox useDeterministe;
    private JCheckBox useColor;
    private JCheckBox displayProgress;
    private JRadioButton byNode;
    private JRadioButton byIteration;
    private JSlider slider;
    private boolean lock=false;
    
    public LabelPropagationPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 1));

        useWeight = new JCheckBox("use weight");
        useDirection = new JCheckBox("use direction");
        useDeterministe = new JCheckBox("use determinism");
        useColor = new JCheckBox("use colors");
        displayProgress = new JCheckBox("display progress");
        
        byNode = new JRadioButton("progress per node");
        byIteration = new JRadioButton("per iteration");
        final int MIN = 0;
        final int MAX = 100;
        final int INIT = 10;
        slider = new JSlider(JSlider.HORIZONTAL, MIN, MAX, INIT);
        slider.setMajorTickSpacing(20);
        slider.setMinorTickSpacing(5);
        slider.setPaintTicks(true);
        Hashtable labelTable = new Hashtable();
        labelTable.put( new Integer( 0 ), new JLabel("Fast") );
        labelTable.put( new Integer( MAX ), new JLabel("Slow") );
        slider.setLabelTable(labelTable);
        slider.setPaintLabels(true);
        
        ButtonGroup group = new ButtonGroup();
        group.add(byNode);
        group.add(byIteration);
        displayProgress.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                lock = !lock;
                byNode.setEnabled(lock);
                byIteration.setEnabled(lock);
                slider.setEnabled(lock);
            }
        });
        
        panel.add(useWeight);
        panel.add(useDirection);
        panel.add(useDeterministe);
        panel.add(useColor);
        panel.add(displayProgress);
        panel.add(byNode);
        panel.add(byIteration);
        panel.add(slider);
        
        add(panel);
    }
    
    LabelPropagation ststcs;
    
    public void setup(LabelPropagation ststcs)
    {   this.ststcs = ststcs;
    
        // set weights and directions settings
        boolean isDirected = false;
        boolean isWeighted = false;
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        if (graphController != null)
        {   GraphModel gm = graphController.getModel();
            if(gm != null)
            {
                //isDirected = graphController.getModel().isDirected();
                if(gm.isMixed()||gm.isDirected()) {
                    isDirected=true;
                }
                Graph g = gm.getGraph();
                isWeighted = isWeighted(g);
            }
        }
        useDirection.setSelected(isDirected);
        useDirection.setEnabled(isDirected);
        useWeight.setSelected(isWeighted);   
        useWeight.setEnabled(isWeighted);  
        
        useDeterministe.setSelected(false);
        
        // set color related settings
        useColor.setSelected(false);
        displayProgress.setSelected(false);
        byNode.setEnabled(false);
        byIteration.setEnabled(false);
        slider.setEnabled(false);
    }
    
       public void unsetup()
       {    
           ststcs.setUseWeight(useWeight.isSelected());
           ststcs.setUseDirection(useDirection.isSelected());
           ststcs.setUseColor(useColor.isSelected());
           ststcs.setUseProgress(displayProgress.isSelected());        
           ststcs.setByNode(byNode.isSelected());        
           ststcs.setByIteration(byIteration.isSelected());   
           ststcs.setSpeed(slider.getValue());
           ststcs.setUseDeterministe(useDeterministe.isSelected());
       }
    
    private boolean isWeighted(Graph g)
    {   boolean result = false;
    
        EdgeIterator it = g.getEdges().iterator();
        while(!result && it.hasNext())
        {   Edge edge = it.next();
            result = edge.getWeight()!=1;
        }
      
        return result;
    }
       
}
