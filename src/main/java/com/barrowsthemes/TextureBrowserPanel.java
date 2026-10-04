package com.barrowsthemes;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import net.runelite.client.ui.PluginPanel;

/** Swing UI only; no client/scene access from event listeners. */
final class TextureBrowserPanel extends PluginPanel
{
	enum Target
	{
		FLOOR("floorTexture"), WALL("wallTexture");
		final String key;
		Target(String key) { this.key = key; }
		@Override public String toString() { return this == FLOOR ? "Floors" : "Walls / scenery"; }
	}

	private final JPanel grid = new JPanel(new GridLayout(0, 3, 4, 4));
	private final JComboBox<Target> target = new JComboBox<>(Target.values());
	private final JLabel status = new JLabel("Refresh while logged in.");
	private final JButton refresh = new JButton("Refresh game textures");
	private final JButton inspect = new JButton("Inspect nearby original scenery");
	private final JButton corners = new JButton("Short wall-corner report");
	private final JTextArea inspection = new JTextArea(12, 18);

	TextureBrowserPanel(Runnable requestRefresh, Runnable requestInspection, Runnable requestCorners, BiConsumer<String, Integer> select)
	{
		add(new JLabel("Barrows materials"));
		add(new JLabel("<html>Nine theme material palettes:<br>three cache-sampled, six artistic.<br>Textures below are manual overrides.</html>"));
		add(new JLabel("<html>Choose a target, then a texture.<br>Wall faces without existing textures<br>keep the selected colour palette.</html>"));
		add(target);
		JButton palette = new JButton("Use theme materials (no override)");
		palette.addActionListener(e -> {
			select.accept(((Target) target.getSelectedItem()).key, -1);
			status.setText("Theme materials: " + target.getSelectedItem());
		});
		add(palette);
		JButton clear = new JButton("Clear both texture overrides");
		clear.addActionListener(e -> {
			select.accept("floorTexture", -1);
			select.accept("wallTexture", -1);
			status.setText("Both texture overrides cleared");
		});
		add(clear);
		refresh.addActionListener(e -> { refresh.setEnabled(false); status.setText("Loading previews..."); requestRefresh.run(); });
		add(refresh);
		add(status);
		inspect.addActionListener(e -> { inspect.setEnabled(false); requestInspection.run(); });
		add(inspect);
		corners.addActionListener(e -> { corners.setEnabled(false); requestCorners.run(); });
		add(corners);
		inspection.setEditable(false);
		inspection.setLineWrap(true);
		inspection.setWrapStyleWord(true);
		inspection.setText("Inspect while standing near an unchanged piece. Select/copy this text to share findings.");
		add(new JScrollPane(inspection));
		add(grid);
		this.select = select;
	}

	private final BiConsumer<String, Integer> select;

	void showTextures(List<TexturePreview> previews)
	{
		grid.removeAll();
		for (TexturePreview preview : previews)
		{
			JButton button = new JButton(Integer.toString(preview.id), new ImageIcon(preview.image.getScaledInstance(48, 48, Image.SCALE_FAST)));
			button.setHorizontalTextPosition(JButton.CENTER);
			button.setVerticalTextPosition(JButton.BOTTOM);
			button.setPreferredSize(new Dimension(64, 76));
			button.setToolTipText("Texture " + preview.id + ": apply to selected target");
			button.addActionListener(e -> {
				select.accept(((Target) target.getSelectedItem()).key, preview.id);
				status.setText("Texture " + preview.id + " selected");
			});
			grid.add(button);
		}
		status.setText(previews.isEmpty() ? "No textures available; log in first." : previews.size() + " game textures available");
		refresh.setEnabled(true);
		revalidate();
		repaint();
	}

	void showInspection(String report)
	{
		inspection.setText(report);
		inspection.setCaretPosition(0);
		inspect.setEnabled(true);
		corners.setEnabled(true);
	}

	static BufferedImage icon()
	{
		BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.setColor(new Color(112, 157, 126));
			graphics.fillRect(3, 3, 26, 26);
			graphics.setColor(new Color(57, 73, 62));
			for (int y = 3; y < 30; y += 8)
			{
				graphics.drawLine(3, y, 28, y);
				for (int x = 3 + ((y / 8) % 2) * 8; x < 29; x += 16) { graphics.drawLine(x, y, x, Math.min(29, y + 8)); }
			}
		}
		finally { graphics.dispose(); }
		return image;
	}
}
