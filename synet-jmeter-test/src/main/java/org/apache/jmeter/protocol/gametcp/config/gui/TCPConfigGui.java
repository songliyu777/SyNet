/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.jmeter.protocol.gametcp.config.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.config.gui.AbstractConfigGui;
import org.apache.jmeter.gui.ServerPanel;
import org.apache.jmeter.gui.util.HorizontalPanel;
import org.apache.jmeter.gui.util.JSyntaxTextArea;
import org.apache.jmeter.gui.util.JTextScrollPane;
import org.apache.jmeter.gui.util.TristateCheckBox;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.protocol.gametcp.sampler.GameTCPSampler;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.gui.JLabeledTextField;

public class TCPConfigGui extends AbstractConfigGui {

    private static final long serialVersionUID = 240L;

    private ServerPanel serverPanel;
    
    private JLabeledTextField classname;

    private JCheckBox reUseConnection;

    private TristateCheckBox setNoDelay;

    private TristateCheckBox closeConnection;

    private JTextField soLinger;

    private JTextField eolByte;

    private JSyntaxTextArea requestData;

    private boolean displayName = true;

    public TCPConfigGui() {
        this(true);
    }

    public TCPConfigGui(boolean displayName) {
        this.displayName = displayName;
        init();
    }

    @Override
    public String getLabelResource() {
        return "tcp_config_title"; // $NON-NLS-1$
    }

    @Override
    public void configure(TestElement element) {
        super.configure(element);
        // N.B. this will be a config element, so we cannot use the getXXX() methods
        classname.setText(element.getPropertyAsString(GameTCPSampler.CLASSNAME));
        serverPanel.setServer(element.getPropertyAsString(GameTCPSampler.SERVER));
        // Default to original behaviour, i.e. re-use connection
        reUseConnection.setSelected(element.getPropertyAsBoolean(GameTCPSampler.RE_USE_CONNECTION, GameTCPSampler.RE_USE_CONNECTION_DEFAULT));
        serverPanel.setPort(element.getPropertyAsString(GameTCPSampler.PORT));
        serverPanel.setResponseTimeout(element.getPropertyAsString(GameTCPSampler.TIMEOUT));
        serverPanel.setConnectTimeout(element.getPropertyAsString(GameTCPSampler.TIMEOUT_CONNECT));
        setNoDelay.setTristateFromProperty(element, GameTCPSampler.NODELAY);
        requestData.setInitialText(element.getPropertyAsString(GameTCPSampler.REQUEST));
        requestData.setCaretPosition(0);
        closeConnection.setTristateFromProperty(element, GameTCPSampler.CLOSE_CONNECTION);
        soLinger.setText(element.getPropertyAsString(GameTCPSampler.SO_LINGER));
        eolByte.setText(element.getPropertyAsString(GameTCPSampler.EOL_BYTE));
    }

    @Override
    public TestElement createTestElement() {
        ConfigTestElement element = new ConfigTestElement();
        modifyTestElement(element);
        return element;
    }

    /**
     * Modifies a given TestElement to mirror the data in the gui components.
     *
     * @see org.apache.jmeter.gui.JMeterGUIComponent#modifyTestElement(TestElement)
     */
    @Override
    public void modifyTestElement(TestElement element) {
        configureTestElement(element);
        // N.B. this will be a config element, so we cannot use the setXXX() methods
        element.setProperty(GameTCPSampler.CLASSNAME, classname.getText(), "");
        element.setProperty(GameTCPSampler.SERVER, serverPanel.getServer());
        element.setProperty(GameTCPSampler.RE_USE_CONNECTION, reUseConnection.isSelected());
        element.setProperty(GameTCPSampler.PORT, serverPanel.getPort());
        setNoDelay.setPropertyFromTristate(element, GameTCPSampler.NODELAY);
        element.setProperty(GameTCPSampler.TIMEOUT, serverPanel.getResponseTimeout());
        element.setProperty(GameTCPSampler.TIMEOUT_CONNECT, serverPanel.getConnectTimeout(),"");
        element.setProperty(GameTCPSampler.REQUEST, requestData.getText());
        closeConnection.setPropertyFromTristate(element, GameTCPSampler.CLOSE_CONNECTION); // Don't use default for saving tristates
        element.setProperty(GameTCPSampler.SO_LINGER, soLinger.getText(), "");
        element.setProperty(GameTCPSampler.EOL_BYTE, eolByte.getText(), "");
    }

    /**
     * Implements JMeterGUIComponent.clearGui
     */
    @Override
    public void clearGui() {
        super.clearGui();

        serverPanel.clear();
        classname.setText(""); //$NON-NLS-1$
        requestData.setInitialText(""); //$NON-NLS-1$
        reUseConnection.setSelected(true);
        setNoDelay.setSelected(false); // TODO should this be indeterminate?
        closeConnection.setSelected(GameTCPSampler.CLOSE_CONNECTION_DEFAULT); // TODO should this be indeterminate?
        soLinger.setText(""); //$NON-NLS-1$
        eolByte.setText(""); //$NON-NLS-1$
    }


    private JPanel createNoDelayPanel() {
        JLabel label = new JLabel(JMeterUtils.getResString("tcp_nodelay")); // $NON-NLS-1$

        setNoDelay = new TristateCheckBox();
        label.setLabelFor(setNoDelay);

        JPanel nodelayPanel = new JPanel(new FlowLayout());
        nodelayPanel.add(label);
        nodelayPanel.add(setNoDelay);
        return nodelayPanel;
    }

    private JPanel createClosePortPanel() {
        JLabel label = new JLabel(JMeterUtils.getResString("reuseconnection")); //$NON-NLS-1$

        reUseConnection = new JCheckBox("", true);
        reUseConnection.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                closeConnection.setEnabled(true);
            } else {
                closeConnection.setEnabled(false);
            }
        });
        label.setLabelFor(reUseConnection);

        JPanel closePortPanel = new JPanel(new FlowLayout());
        closePortPanel.add(label);
        closePortPanel.add(reUseConnection);
        return closePortPanel;
    }

    private JPanel createCloseConnectionPanel() {
        JLabel label = new JLabel(JMeterUtils.getResString("closeconnection")); // $NON-NLS-1$

        closeConnection = new TristateCheckBox("", GameTCPSampler.CLOSE_CONNECTION_DEFAULT);
        label.setLabelFor(closeConnection);

        JPanel closeConnectionPanel = new JPanel(new FlowLayout());
        closeConnectionPanel.add(label);
        closeConnectionPanel.add(closeConnection);
        return closeConnectionPanel;
    }

    private JPanel createSoLingerOption() {
        JLabel label = new JLabel(JMeterUtils.getResString("solinger")); //$NON-NLS-1$ 

        soLinger = new JTextField(5); // 5 columns size
        soLinger.setMaximumSize(new Dimension(soLinger.getPreferredSize()));
        label.setLabelFor(soLinger);

        JPanel soLingerPanel = new JPanel(new FlowLayout());
        soLingerPanel.add(label);
        soLingerPanel.add(soLinger);
        return soLingerPanel;
    }

    private JPanel createEolBytePanel() {
        JLabel label = new JLabel(JMeterUtils.getResString("eolbyte")); //$NON-NLS-1$
        label.setText("end of command");

        eolByte = new JTextField(3); // 3 columns size
        eolByte.setMaximumSize(new Dimension(eolByte.getPreferredSize()));
        label.setLabelFor(eolByte);

        JPanel eolBytePanel = new JPanel(new FlowLayout());
        eolBytePanel.add(label);
        eolBytePanel.add(eolByte);
        return eolBytePanel;
    }

    private JPanel createRequestPanel() {
        JLabel reqLabel = new JLabel(JMeterUtils.getResString("tcp_request_data")); // $NON-NLS-1$
        requestData = JSyntaxTextArea.getInstance(15, 80);
        requestData.setLanguage("text"); //$NON-NLS-1$
        reqLabel.setLabelFor(requestData);

        JPanel reqDataPanel = new JPanel(new BorderLayout(5, 0));
        reqDataPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));

        reqDataPanel.add(reqLabel, BorderLayout.WEST);
        reqDataPanel.add(JTextScrollPane.getInstance(requestData), BorderLayout.CENTER);
        return reqDataPanel;
    }

    private void init() { // WARNING: called from ctor so must not be overridden (i.e. must be private or final)
        setLayout(new BorderLayout(0, 5));

        serverPanel = new ServerPanel();
        
        if (displayName) {
            setBorder(makeBorder());
            add(makeTitlePanel(), BorderLayout.NORTH);
        }

        VerticalPanel mainPanel = new VerticalPanel();
        classname = new JLabeledTextField(JMeterUtils.getResString("tcp_classname")); // $NON-NLS-1$
        mainPanel.add(classname);
        mainPanel.add(serverPanel);
        
        HorizontalPanel optionsPanel = new HorizontalPanel();
        optionsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));
        optionsPanel.add(createClosePortPanel());
        optionsPanel.add(createCloseConnectionPanel());
        optionsPanel.add(createNoDelayPanel());
        optionsPanel.add(createSoLingerOption());
        optionsPanel.add(createEolBytePanel());
        mainPanel.add(optionsPanel);
        mainPanel.add(createRequestPanel());

        add(mainPanel, BorderLayout.CENTER);
    }
}
