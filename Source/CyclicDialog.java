//	CyclicDialog.java - Simple dialog for configuring cyclic timeline display.

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class CyclicDialog {

	protected static final String TITLE = "Cyclic View";
	protected static final String CYCLIC_LABEL = "Cyclic view";
	protected static final String YEAR_LABEL = "Year";

	public static void doDialog(TLDocument doc, TLWindow parentWindow) {
		if (parentWindow == null)
			return;

		final JCheckBox cyclicBox = new JCheckBox(CYCLIC_LABEL, parentWindow.iTPM.isCyclicView());
		final JTextField yearField = new JTextField(String.valueOf(parentWindow.iTPM.iCyclicYear), 6);
		yearField.setEnabled(cyclicBox.isSelected());

		cyclicBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				yearField.setEnabled(cyclicBox.isSelected());
			}
		});

		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridx = 0;
		gbc.gridy = 0;
		panel.add(cyclicBox, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		panel.add(new JLabel(YEAR_LABEL), gbc);
		gbc.gridx = 1;
		gbc.gridy = 1;
		panel.add(yearField, gbc);

		while (true) {
			int answer = JOptionPane.showConfirmDialog(parentWindow, panel, TITLE,
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (answer != JOptionPane.OK_OPTION)
				return;

			try {
				int year = Integer.parseInt(yearField.getText().trim());
				if (year <= 0)
					throw new NumberFormatException("Year must be positive");

				parentWindow.iTPM.iCyclicView = cyclicBox.isSelected();
				parentWindow.iTPM.iCyclicYear = year;
				return;
			}
			catch (NumberFormatException ex) {
				JOptionPane.showMessageDialog(parentWindow,
						"Please enter a positive year for cyclic display.",
						"Invalid Year", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
}
