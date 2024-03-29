package apps.player;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import player.GamePlayer;
import player.gamer.SingleGameGamer;
import util.reflection.ProjectSearcher;
import util.ui.NativeUI;
import apps.player.config.ConfigPanel;
import apps.player.detail.DetailPanel;
import apps.player.match.MatchPanel;
import apps.player.network.NetworkPanel;

@SuppressWarnings("serial")
public final class PlayerPanel extends JPanel
{
	private static void createAndShowGUI(PlayerPanel playerPanel)
	{
		JFrame frame = new JFrame("Game Player");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.setPreferredSize(new Dimension(1024, 768));
		frame.getContentPane().add(playerPanel);

		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args) throws IOException
	{
	    NativeUI.setNativeUI();	    

	    final PlayerPanel playerPanel = new PlayerPanel();
	    javax.swing.SwingUtilities.invokeLater(new Runnable()
	    {

		public void run()
		{
		    createAndShowGUI(playerPanel);
		}
	    });
	}

	private final JButton createButton;
	private final JTabbedPane playersTabbedPane;

	private final JTextField portTextField;

	private final JComboBox typeComboBox;
	
	private Integer defaultPort = 9147;
	
	private List<Class<?>> gamers = ProjectSearcher.getAllClassesThatAre(SingleGameGamer.class);

	public PlayerPanel()
	{
		super(new GridBagLayout());

		portTextField = new JTextField(defaultPort.toString());
		typeComboBox = new JComboBox();
		createButton = new JButton(createButtonMethod());
		playersTabbedPane = new JTabbedPane();

		portTextField.setColumns(15);

		List<Class<?>> gamersCopy = new ArrayList<Class<?>>(gamers);
		for(Class<?> gamer : gamersCopy)
		{
			SingleGameGamer g;
			try {
				g = (SingleGameGamer) gamer.newInstance();
				typeComboBox.addItem(g.getName());
			} catch(Exception ex) {
			    gamers.remove(gamer);
			}
		}

		JPanel managerPanel = new JPanel(new GridBagLayout());
		managerPanel.setBorder(new TitledBorder("Manager"));

		managerPanel.add(new JLabel("Port:"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(20, 5, 5, 5), 5, 5));
		managerPanel.add(portTextField, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(20, 5, 5, 5), 5, 5));
		managerPanel.add(new JLabel("Type:"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		managerPanel.add(typeComboBox, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
		managerPanel.add(createButton, new GridBagConstraints(1, 2, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

		JPanel playersPanel = new JPanel(new GridBagLayout());
		playersPanel.setBorder(new TitledBorder("Players"));

		playersPanel.add(playersTabbedPane, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));

		this.add(managerPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
		this.add(playersPanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
	}

	private AbstractAction createButtonMethod()
	{
		return new AbstractAction("Create")
		{

			public void actionPerformed(ActionEvent evt)
			{
				try
				{
					int port = Integer.valueOf(portTextField.getText());
					String type = (String) typeComboBox.getSelectedItem();

					MatchPanel matchPanel = new MatchPanel();
					NetworkPanel networkPanel = new NetworkPanel();
					DetailPanel detailPanel = null;
					ConfigPanel configPanel = null;
					SingleGameGamer gamer = null;

					Class<?> gamerClass = gamers.get(typeComboBox.getSelectedIndex());
					try {
						gamer = (SingleGameGamer) gamerClass.newInstance();
					} catch(Exception ex) { throw new RuntimeException(ex); }
					detailPanel = gamer.getDetailPanel();
					configPanel = gamer.getConfigPanel();

					gamer.addObserver(matchPanel);
					gamer.addObserver(detailPanel);

					GamePlayer player = new GamePlayer(port, gamer);
					player.addObserver(networkPanel);
					player.start();					

					JTabbedPane tab = new JTabbedPane();
					tab.addTab("Match", matchPanel);
					tab.addTab("Network", networkPanel);
					tab.addTab("Configuration", configPanel);
					tab.addTab("Detail", detailPanel);
					playersTabbedPane.addTab(type + " (" + player.getGamerPort() + ")", tab);
					playersTabbedPane.setSelectedIndex(playersTabbedPane.getTabCount()-1);
					
					defaultPort++;
					portTextField.setText(defaultPort.toString());
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		};
	}
}