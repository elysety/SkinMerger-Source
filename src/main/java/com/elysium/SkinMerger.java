//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.RasterFormatException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class SkinMerger extends JFrame {
    private final JTextField headUserField = new JTextField(15);
    private final JTextField bodyUserField = new JTextField(15);
    private final JLabel previewLabel = new JLabel("Merged Skin Preview (64x64 PNG)", 0);
    private final JButton mergeButton = new JButton("Merge and Preview Skin");
    private final JButton saveButton = new JButton("Save Merged Skin (.png)");
    private BufferedImage mergedSkinImage = null;
    private static final int SKIN_WIDTH = 64;
    private static final int SKIN_HEIGHT = 64;
    private static final String USER_AGENT = "SkinMergerApp/1.0 (Java)";
    private static final int CONNECT_TIMEOUT = 8000;
    private static final int READ_TIMEOUT = 10000;
    private final Map<String, BufferedImage> skinCache = new ConcurrentHashMap();

    public SkinMerger() {
        this.setTitle("Minecraft Skin Head Swap Utility");
        this.setDefaultCloseOperation(3);
        this.setLayout(new BorderLayout(10, 10));
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inputPanel.add(new JLabel(" Head Donor Username (Skin 1):"));
        inputPanel.add(this.headUserField);
        inputPanel.add(new JLabel(" Body Donor Username (Skin 2):"));
        inputPanel.add(this.bodyUserField);
        this.mergeButton.addActionListener(this::handleMergeAction);
        this.saveButton.addActionListener(this::handleSaveAction);
        this.saveButton.setEnabled(false);
        inputPanel.add(this.mergeButton);
        inputPanel.add(this.saveButton);
        this.add(inputPanel, "North");
        this.previewLabel.setPreferredSize(new Dimension(256, 256));
        this.previewLabel.setOpaque(true);
        this.previewLabel.setBackground(new Color(240, 240, 240));
        this.previewLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY, 1), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        this.previewLabel.setFont(new Font("Monospaced", 1, 14));
        this.add(this.previewLabel, "Center");
        JLabel infoLabel = new JLabel("A merged skin uses the head (including hat) from Skin 1 onto the body of Skin 2.", 0);
        infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        this.add(infoLabel, "South");
        this.pack();
        this.setLocationRelativeTo((Component)null);
    }

    private void handleMergeAction(ActionEvent event) {
        final String headUser = this.headUserField.getText().trim();
        final String bodyUser = this.bodyUserField.getText().trim();
        if (!headUser.isEmpty() && !bodyUser.isEmpty()) {
            this.setControlsEnabled(false);
            SwingWorker<BufferedImage, Void> worker = new SwingWorker<BufferedImage, Void>() {
                private String notice;

                {
                    Objects.requireNonNull(SkinMerger.this);
                    this.notice = null;
                }

                protected BufferedImage doInBackground() {
                    try {
                        BufferedImage headSkin = SkinMerger.this.loadSkinForUser(headUser);
                        BufferedImage bodySkin = SkinMerger.this.loadSkinForUser(bodyUser);
                        if (headSkin == null || bodySkin == null) {
                            this.notice = "One or both skins failed to download; placeholders were used.";
                            if (headSkin == null) {
                                headSkin = SkinMerger.this.makeFallbackSkin(headUser);
                            }

                            if (bodySkin == null) {
                                bodySkin = SkinMerger.this.makeFallbackSkin(bodyUser);
                            }
                        }

                        headSkin = SkinMerger.this.ensure64x64(headSkin);
                        bodySkin = SkinMerger.this.ensure64x64(bodySkin);
                        return SkinMerger.this.mergeSkins(headSkin, bodySkin);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        this.notice = "Unexpected error: " + ex.getMessage();
                        return SkinMerger.this.makeFallbackSkin("merged_fallback");
                    }
                }

                protected void done() {
                    try {
                        SkinMerger.this.mergedSkinImage = (BufferedImage)this.get();
                        Image scaled = SkinMerger.this.mergedSkinImage.getScaledInstance(SkinMerger.this.previewLabel.getWidth() - 10, SkinMerger.this.previewLabel.getHeight() - 10, 4);
                        SkinMerger.this.previewLabel.setIcon(new ImageIcon(scaled));
                        SkinMerger.this.previewLabel.setText("Merged Skin Ready");
                        SkinMerger.this.saveButton.setEnabled(true);
                        if (this.notice != null) {
                            JOptionPane.showMessageDialog(SkinMerger.this, this.notice, "Notice", 1);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(SkinMerger.this, "Failed to merge skins: " + ex.getMessage(), "Error", 0);
                    } finally {
                        SkinMerger.this.setControlsEnabled(true);
                    }

                }
            };
            worker.execute();
        } else {
            JOptionPane.showMessageDialog(this, "Please enter both usernames.", "Missing Input", 2);
        }
    }

    private void handleSaveAction(ActionEvent event) {
        if (this.mergedSkinImage == null) {
            JOptionPane.showMessageDialog(this, "Please merge a skin first.", "Save Error", 2);
        } else {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save Merged Minecraft Skin");
            String var10003 = this.headUserField.getText();
            chooser.setSelectedFile(new File(var10003 + "_head_on_" + this.bodyUserField.getText() + "_body.png"));
            if (chooser.showSaveDialog(this) == 0) {
                File f = chooser.getSelectedFile();
                if (!f.getName().toLowerCase().endsWith(".png")) {
                    f = new File(f.getAbsolutePath() + ".png");
                }

                try {
                    ImageIO.write(this.mergedSkinImage, "png", f);
                    JOptionPane.showMessageDialog(this, "Saved to: " + f.getAbsolutePath(), "Save Success", 1);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Save error: " + e.getMessage(), "Save Error", 0);
                    e.printStackTrace();
                }
            }

        }
    }

    private void setControlsEnabled(boolean enabled) {
        this.headUserField.setEnabled(enabled);
        this.bodyUserField.setEnabled(enabled);
        this.mergeButton.setEnabled(enabled);
        if (!enabled) {
            this.saveButton.setEnabled(false);
        }

    }

    private BufferedImage mergeSkins(BufferedImage headSkin, BufferedImage bodySkin) {
        BufferedImage result = new BufferedImage(64, 64, 2);
        Graphics2D g = result.createGraphics();
        g.drawImage(bodySkin, 0, 0, (ImageObserver)null);
        int[][] regions = new int[][]{{8, 0, 8, 8}, {16, 0, 8, 8}, {0, 8, 8, 8}, {8, 8, 8, 8}, {16, 8, 8, 8}, {24, 8, 8, 8}, {40, 0, 8, 8}, {48, 0, 8, 8}, {32, 8, 8, 8}, {40, 8, 8, 8}, {48, 8, 8, 8}, {56, 8, 8, 8}};

        for(int[] r : regions) {
            try {
                BufferedImage part = headSkin.getSubimage(r[0], r[1], r[2], r[3]);
                g.drawImage(part, r[0], r[1], (ImageObserver)null);
            } catch (RasterFormatException ex) {
                System.err.println("Warning: head region out of bounds: " + ex.getMessage());
            }
        }

        g.dispose();
        return result;
    }

    private BufferedImage loadSkinForUser(String username) {
        if (username != null && !username.isEmpty()) {
            String key = username.toLowerCase();
            if (this.skinCache.containsKey(key)) {
                return (BufferedImage)this.skinCache.get(key);
            } else {
                try {
                    String uuid = this.fetchUuid(username);
                    if (uuid == null) {
                        System.out.println("UUID lookup failed for: " + username);
                        BufferedImage fallback = this.makeFallbackSkin(username);
                        this.skinCache.put(key, fallback);
                        return fallback;
                    } else {
                        String profileJson = this.fetchProfileJson(uuid);
                        if (profileJson == null) {
                            System.out.println("Profile fetch failed for uuid: " + uuid);
                            BufferedImage fallback = this.makeFallbackSkin(username);
                            this.skinCache.put(key, fallback);
                            return fallback;
                        } else {
                            String base64 = this.extractPropertyValue(profileJson);
                            if (base64 == null) {
                                System.out.println("No 'value' property in profile JSON for uuid: " + uuid);
                                BufferedImage fallback = this.makeFallbackSkin(username);
                                this.skinCache.put(key, fallback);
                                return fallback;
                            } else {
                                String decoded = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
                                String skinUrl = this.extractSkinUrl(decoded);
                                if (skinUrl == null) {
                                    System.out.println("No skin URL in textures JSON for user: " + username);
                                    BufferedImage fallback = this.makeFallbackSkin(username);
                                    this.skinCache.put(key, fallback);
                                    return fallback;
                                } else {
                                    BufferedImage skinImage = this.downloadImage(skinUrl);
                                    if (skinImage == null) {
                                        System.out.println("Failed to download skin image from: " + skinUrl);
                                        BufferedImage fallback = this.makeFallbackSkin(username);
                                        this.skinCache.put(key, fallback);
                                        return fallback;
                                    } else {
                                        this.skinCache.put(key, skinImage);
                                        return skinImage;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    BufferedImage fallback = this.makeFallbackSkin(username);
                    this.skinCache.put(key, fallback);
                    return fallback;
                }
            }
        } else {
            return null;
        }
    }

    private String fetchUuid(String username) throws IOException {
        String urlStr = "https://api.mojang.com/users/profiles/minecraft/" + URLEncoder.encode(username, StandardCharsets.UTF_8.name());
        HttpURLConnection conn = this.openConnection(urlStr);
        int code = conn.getResponseCode();
        if (code != 204 && code != 404) {
            if (code != 200) {
                System.out.println("UUID lookup HTTP " + code + " for username: " + username);
                return null;
            } else {
                String body = this.readAll(conn.getInputStream());
                Pattern p = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F]+)\"");
                Matcher m = p.matcher(body);
                return m.find() ? m.group(1) : null;
            }
        } else {
            return null;
        }
    }

    private String fetchProfileJson(String uuid) throws IOException {
        String urlStr = "https://sessionserver.mojang.com/session/minecraft/profile/" + URLEncoder.encode(uuid, StandardCharsets.UTF_8.name());
        HttpURLConnection conn = this.openConnection(urlStr);
        int code = conn.getResponseCode();
        if (code != 200) {
            System.out.println("Profile fetch HTTP " + code + " for uuid: " + uuid);
            return null;
        } else {
            return this.readAll(conn.getInputStream());
        }
    }

    private String extractPropertyValue(String profileJson) {
        Pattern p = Pattern.compile("\"value\"\\s*:\\s*\"([A-Za-z0-9+/=]+)\"");
        Matcher m = p.matcher(profileJson);
        return m.find() ? m.group(1) : null;
    }

    private String extractSkinUrl(String texturesJson) {
        Pattern p = Pattern.compile("\"url\"\\s*:\\s*\"(https?:\\\\?/\\\\?/[^\"}]+)\"");
        Matcher m = p.matcher(texturesJson);
        if (m.find()) {
            String raw = m.group(1);
            return raw.replace("\\/", "/");
        } else {
            p = Pattern.compile("\"url\"\\s*:\\s*\"(https?://[^\"]+)\"");
            m = p.matcher(texturesJson);
            return m.find() ? m.group(1) : null;
        }
    }

    private BufferedImage downloadImage(String urlStr) {
        try {
            HttpURLConnection conn = this.openConnection(urlStr);
            int code = conn.getResponseCode();
            if (code >= 300 && code < 400) {
                String loc = conn.getHeaderField("Location");
                if (loc != null && !loc.isEmpty()) {
                    conn = this.openConnection(loc);
                    code = conn.getResponseCode();
                }
            }

            if (code != 200) {
                System.out.println("Download HTTP " + code + " for URL: " + urlStr);
                return null;
            } else {
                try (InputStream is = conn.getInputStream()) {
                    return ImageIO.read(is);
                }
            }
        } catch (Exception ex) {
            System.out.println("Error downloading image: " + ex.getMessage());
            return null;
        }
    }

    private HttpURLConnection openConnection(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "SkinMergerApp/1.0 (Java)");
        conn.setInstanceFollowRedirects(false);
        return conn;
    }

    private String readAll(InputStream is) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();

            String line;
            while((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }

            return sb.toString();
        }
    }

    private BufferedImage ensure64x64(BufferedImage img) {
        if (img == null) {
            return this.makeFallbackSkin("null_skin");
        } else if (img.getWidth() == 64 && img.getHeight() == 64) {
            return img;
        } else if (img.getWidth() == 64 && img.getHeight() == 32) {
            BufferedImage n = new BufferedImage(64, 64, 2);
            Graphics2D g = n.createGraphics();
            g.drawImage(img, 0, 0, (ImageObserver)null);

            try {
                g.drawImage(img.getSubimage(0, 16, 32, 16), 0, 32, (ImageObserver)null);
                g.drawImage(img.getSubimage(32, 0, 32, 16), 32, 32, (ImageObserver)null);
            } catch (RasterFormatException var5) {
            }

            g.dispose();
            return n;
        } else {
            int var10001 = img.getWidth();
            return this.makeFallbackSkin("bad_size_" + var10001 + "x" + img.getHeight());
        }
    }

    private BufferedImage makeFallbackSkin(String label) {
        BufferedImage img = new BufferedImage(64, 64, 2);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(75, 110, 180));
        g.fillRect(0, 0, 64, 64);
        g.setColor(new Color(220, 200, 80));
        g.fillRect(16, 8, 8, 8);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", 1, 9));
        String text = label != null && !label.isEmpty() ? label : "fallback";
        text = text.length() > 10 ? text.substring(0, 10) : text;
        g.drawString(text, 2, 12);
        g.dispose();
        return img;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SkinMerger app = new SkinMerger();
            app.setVisible(true);
            System.out.println("SkinMerger started. Enter usernames and click Merge.");
        });
    }
}
