package com.nightauth.desktop;

import com.nightauth.sdk.NightAuthApi;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public final class Main extends JFrame {

    public static final Color COLOR_BG_DARK       = new Color(5, 5, 6);
    public static final Color COLOR_CARD_BG       = new Color(13, 13, 16);
    public static final Color COLOR_CARD_HEADER   = new Color(19, 19, 22);
    public static final Color COLOR_BORDER_SUBTLE = new Color(38, 38, 43);
    public static final Color COLOR_HERO_BG       = new Color(13, 16, 26);
    public static final Color COLOR_INPUT_BG      = new Color(14, 16, 23);
    public static final Color COLOR_INPUT_HOVER   = new Color(18, 21, 32);
    public static final Color COLOR_INPUT_BORDER  = new Color(35, 39, 54);
    public static final Color COLOR_INPUT_BORDER_HOVER = new Color(63, 71, 94);

    public static final Color COLOR_ACCENT_BLUE   = new Color(59, 130, 246);
    public static final Color COLOR_BLUE_PRIMARY  = new Color(37, 99, 235);
    public static final Color COLOR_BLUE_LIGHT    = new Color(147, 197, 253);
    public static final Color COLOR_BLUE_GLOW     = new Color(96, 165, 250);
    public static final Color COLOR_BLUE_SUBTLE   = new Color(17, 26, 46);
    public static final Color COLOR_BLUE_BORDER   = new Color(37, 62, 112);
    public static final Color COLOR_ACCENT_PURPLE = new Color(139, 92, 246);

    public static final Color COLOR_TEXT_HEAD     = new Color(244, 244, 245);
    public static final Color COLOR_TEXT_BODY     = new Color(226, 232, 240);
    public static final Color COLOR_TEXT_SEC      = new Color(161, 161, 170);
    public static final Color COLOR_TEXT_MUTED    = new Color(107, 107, 116);
    public static final Color COLOR_TEXT_DIM      = new Color(126, 139, 166);

    public static final Color COLOR_SUCCESS       = new Color(34, 197, 94);
    public static final Color COLOR_SUCCESS_BG    = new Color(12, 42, 32);
    public static final Color COLOR_SUCCESS_BORDER= new Color(24, 92, 70);
    public static final Color COLOR_ERROR         = new Color(244, 63, 94);
    public static final Color COLOR_DANGER        = new Color(220, 38, 38);

    public static final Font FONT_BRAND       = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_WINDOW_SUB  = new Font("Segoe UI", Font.BOLD, 9);
    public static final Font FONT_HERO_TITLE  = new Font("Segoe UI", Font.BOLD, 21);
    public static final Font FONT_DASH_TITLE  = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_SECTION     = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_HEADING     = new Font("Segoe UI", Font.BOLD, 17);
    public static final Font FONT_LABEL       = new Font("Segoe UI", Font.BOLD, 10);
    public static final Font FONT_BODY        = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_BODY_SM     = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_INPUT       = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_STATUS      = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_MONO        = new Font("Consolas", Font.PLAIN, 12);
    public static final Font FONT_MONO_SM     = new Font("Consolas", Font.PLAIN, 10);

    private static final DateTimeFormatter UTC_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    public static final String APP_NAME    = "";
    public static final String APP_API_KEY = "";
    public static final String APP_VERSION = "1.0";
    public static final String APP_TOKEN   = "";

    private NightAuthApi api;
    private boolean authenticated = false;
    private String configuredAppName = APP_NAME;
    private String configuredAppVer  = APP_VERSION;
    private String localHwid = "Detecting...";

    private Point initialClick;

    private final CardLayout rootCardLayout = new CardLayout();
    private final JPanel rootCardPanel = new JPanel(rootCardLayout);

    private JLabel lblHeaderTitle;
    private JLabel lblHeaderSub;

    private enum AuthMode {
        LOGIN_CREDENTIALS,
        LOGIN_LICENSE,
        REGISTER,
        UPGRADE,
        RESET_HWID,
        FORGOT_PASSWORD,
        RESET_PASSWORD_TOKEN
    }
    private AuthMode currentAuthMode = AuthMode.LOGIN_CREDENTIALS;

    private ModernTextField txtUsername;
    private ModernPasswordField txtPassword;
    private ModernTextField txt2faCode;
    private ModernTextField txtEmail;
    private ModernTextField txtResetToken;
    private ModernTextField txtLicenseKey;

    private JPanel panelUsernameGroup;
    private JPanel panelPasswordGroup;
    private JPanel panel2faGroup;
    private JPanel panelEmailGroup;
    private JPanel panelTokenGroup;
    private JPanel panelLicenseGroup;

    private JLabel lblAuthHeading;
    private JLabel lblAuthSubheading;
    private JLabel lblLicenseHint;
    private JLabel lblAuthError;
    private ModernButton btnAuthSubmit;
    private ModernButton btnSwitchMode;
    private ModernButton btnForgotLink;
    private ModernButton btnForgotToggleToken;
    private JPanel segmentedTabContainer;
    private final List<ModernButton> tabButtons = new ArrayList<>();

    private final CardLayout dashContentLayout = new CardLayout();
    private final ScrollablePanel dashContentPanel = new ScrollablePanel(dashContentLayout);
    private JScrollPane dashScrollPane;
    private final List<ModernButton> navButtons = new ArrayList<>();
    private JLabel lblSidebarUsername;
    private JLabel lblStatusText;

    private JPanel panelOverview;
    private JPanel panelSubscriptions;
    private JPanel panelVariables;
    private JPanel panelChat;
    private JPanel panelStats;
    private JPanel panelSecurity;
    private JPanel panelFilesHooks;

    private JPanel panelSubsList;
    private PillBadge badgeActiveSubsCount;

    private JPanel panelUserVarsList;

    private JPanel chatMessagesContainer;
    private JScrollPane scrollChatMessages;
    private JPanel chatEmptyPlaceholder;
    private ModernTextField txtChatRoom;
    private ModernTextField txtChatMessage;
    private PillBadge badgeRoomStatus;
    private String currentChatChannel = null;

    private JPanel panel2faStart;
    private JPanel panel2faQr;
    private JPanel panel2faBackup;
    private JPanel panel2faDisable;
    private JLabel lbl2faQrImage;
    private JLabel txt2faSecretLabel;
    private ModernTextField txt2faConfirmCode;
    private JPanel backupCodesContainer;
    private PillBadge badge2faStatus;
    private ModernPasswordField txt2faDisablePassword;
    private ModernTextField txt2faDisableCode;

    public Main() {
        setUndecorated(true);
        setResizable(false);
        setSize(1020, 680);
        setPreferredSize(new Dimension(1020, 680));
        setLocationRelativeTo(null);
        setBackground(COLOR_BG_DARK);

        detectHardwareId();
        initSdk();
        buildWindow();
    }

    private void detectHardwareId() {
        try {
            localHwid = NightAuthApi.getHardwareId();
            if (localHwid == null || localHwid.trim().isEmpty()) {
                localHwid = "Detected on Auth";
            }
        } catch (Exception e) {
            localHwid = "Detected on Auth";
        }
    }

    private void initSdk() {
        String envName = System.getenv("NIGHTAUTH_APP_NAME");
        String envKey = System.getenv("NIGHTAUTH_API_KEY");
        configuredAppName = (envName != null && !envName.isEmpty()) ? envName : APP_NAME;
        String appKey = (envKey != null && !envKey.isEmpty()) ? envKey : APP_API_KEY;
        configuredAppVer = System.getenv().getOrDefault("NIGHTAUTH_APP_VERSION", APP_VERSION);
        String baseUrl = System.getenv().getOrDefault("NIGHTAUTH_BASE_URL", NightAuthApi.DEFAULT_API_URL);
        boolean loopback = "1".equals(System.getenv("NIGHTAUTH_ALLOW_INSECURE_LOOPBACK"));

        if (configuredAppName == null || configuredAppName.isEmpty() || appKey == null || appKey.isEmpty()) {
            updateStatus("Application credentials are not configured. Please specify your app credentials from the NightAuth Dashboard.", COLOR_ERROR);
            return;
        }
        try {
            api = new NightAuthApi(configuredAppName, appKey, configuredAppVer, null, baseUrl, loopback);
            updateStatus("Establishing secure zero-trust session with cloud...", COLOR_BLUE_GLOW);
            new Thread(() -> {
                try {
                    api.init();
                    if (api.response.success) {
                        updateStatus("Connected securely. Ready to authenticate.", COLOR_SUCCESS);
                    } else {
                        updateStatus(api.response.message != null ? api.response.message : "Handshake refused.", COLOR_ERROR);
                    }
                } catch (Exception e) {
                    updateStatus("Handshake failed: " + e.getMessage(), COLOR_ERROR);
                }
            }).start();
        } catch (Exception e) {
            updateStatus("SDK Init Error: " + e.getMessage(), COLOR_ERROR);
        }
    }

    private static ImageIcon loadLogoIcon(int width, int height) {
        String[] candidates = {
            "logo.png",
            "source/desktop/logo.png",
            "clients/sdks/NightAuth-Java-SDK/source/desktop/logo.png",
            "../source/desktop/logo.png"
        };
        try {
            InputStream is = Main.class.getResourceAsStream("/logo.png");
            if (is == null) is = Main.class.getResourceAsStream("logo.png");
            if (is != null) {
                BufferedImage img = ImageIO.read(is);
                if (img != null) {
                    return new ImageIcon(img.getScaledInstance(width, height, Image.SCALE_SMOOTH));
                }
            }
        } catch (Exception ignored) {}

        for (String path : candidates) {
            try {
                File f = new File(path);
                if (f.exists()) {
                    BufferedImage img = ImageIO.read(f);
                    if (img != null) {
                        return new ImageIcon(img.getScaledInstance(width, height, Image.SCALE_SMOOTH));
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    public static String formatTimestamp(String raw) {
        if (raw == null || raw.trim().isEmpty() || "0".equals(raw.trim())) {
            return "—";
        }
        String s = raw.trim();
        try {
            long epoch = Long.parseLong(s);
            if (epoch > 0) {
                if (epoch > 100_000_000_000L) epoch /= 1000;
                return UTC_FORMATTER.format(Instant.ofEpochSecond(epoch));
            }
        } catch (NumberFormatException ignored) {}

        try {
            Instant inst = Instant.parse(s);
            return UTC_FORMATTER.format(inst);
        } catch (Exception ignored) {}

        try {
            ZonedDateTime zdt = ZonedDateTime.parse(s);
            return UTC_FORMATTER.format(zdt.toInstant());
        } catch (Exception ignored) {}

        try {
            String iso = s.replace(' ', 'T');
            if (!iso.endsWith("Z") && !iso.contains("+") && iso.indexOf("-", 10) == -1) {
                iso += "Z";
            }
            Instant inst = Instant.parse(iso);
            return UTC_FORMATTER.format(inst);
        } catch (Exception ignored) {}

        return s;
    }

    public static String formatTimeRemaining(long totalSeconds) {
        if (totalSeconds <= 0) return "Expired";
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (days >= 1) {
            return hours > 0 ? (days + "d " + hours + "h (" + totalSeconds + "s)") : (days + "d (" + totalSeconds + "s)");
        }
        if (hours >= 1) {
            return minutes > 0 ? (hours + "h " + minutes + "m (" + totalSeconds + "s)") : (hours + "h (" + totalSeconds + "s)");
        }
        if (minutes >= 1) {
            return seconds > 0 ? (minutes + "m " + seconds + "s (" + totalSeconds + "s)") : (minutes + "m (" + totalSeconds + "s)");
        }
        return totalSeconds + "s";
    }

    private void buildWindow() {
        JPanel windowRoot = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_BG_DARK);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.setColor(COLOR_BORDER_SUBTLE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        windowRoot.setOpaque(false);

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setPreferredSize(new Dimension(1020, 42));
        titleBar.setBackground(COLOR_CARD_HEADER);
        titleBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER_SUBTLE),
            new EmptyBorder(0, 16, 0, 10)
        ));

        titleBar.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { initialClick = e.getPoint(); }
        });
        titleBar.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                int thisX = getLocation().x;
                int thisY = getLocation().y;
                int xMoved = e.getX() - initialClick.x;
                int yMoved = e.getY() - initialClick.y;
                setLocation(thisX + xMoved, thisY + yMoved);
            }
        });

        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        brandPanel.setOpaque(false);

        ImageIcon logoIcon = loadLogoIcon(26, 26);
        if (logoIcon != null) {
            JLabel lblLogo = new JLabel(logoIcon);
            brandPanel.add(lblLogo);
        }

        JPanel brandTitles = new JPanel(new GridLayout(2, 1, 0, 0));
        brandTitles.setOpaque(false);
        lblHeaderTitle = new JLabel("NIGHTAUTH");
        lblHeaderTitle.setFont(FONT_BRAND);
        lblHeaderTitle.setForeground(Color.WHITE);

        lblHeaderSub = new JLabel("NATIVE JAVA / SECURITY CLIENT");
        lblHeaderSub.setFont(FONT_WINDOW_SUB);
        lblHeaderSub.setForeground(COLOR_TEXT_MUTED);

        brandTitles.add(lblHeaderTitle);
        brandTitles.add(lblHeaderSub);
        brandPanel.add(brandTitles);

        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 5));
        controlsPanel.setOpaque(false);

        ModernButton btnMin = ModernButton.vectorTitleButton(false, e -> setState(Frame.ICONIFIED));
        ModernButton btnClose = ModernButton.vectorTitleButton(true, e -> {
            if (api != null) {
                try { api.close(); } catch (Exception ignored) {}
            }
            System.exit(0);
        });

        controlsPanel.add(btnMin);
        controlsPanel.add(btnClose);

        titleBar.add(brandPanel, BorderLayout.WEST);
        titleBar.add(controlsPanel, BorderLayout.EAST);
        windowRoot.add(titleBar, BorderLayout.NORTH);

        rootCardPanel.setOpaque(false);
        rootCardPanel.add(buildAuthView(), "AUTH");
        rootCardPanel.add(buildDashboardView(), "DASHBOARD");
        rootCardLayout.show(rootCardPanel, "AUTH");

        windowRoot.add(rootCardPanel, BorderLayout.CENTER);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setPreferredSize(new Dimension(1020, 32));
        statusBar.setBackground(COLOR_CARD_HEADER);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_BORDER_SUBTLE),
            new EmptyBorder(0, 18, 0, 18)
        ));

        lblStatusText = new JLabel(" ");
        lblStatusText.setFont(FONT_STATUS);
        lblStatusText.setForeground(COLOR_TEXT_MUTED);
        statusBar.add(lblStatusText, BorderLayout.WEST);

        windowRoot.add(statusBar, BorderLayout.SOUTH);
        setContentPane(windowRoot);
    }

    private JPanel buildAuthView() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);
        outer.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel splitGrid = new JPanel(new GridBagLayout());
        splitGrid.setOpaque(false);

        GridBagConstraints gbcHero = new GridBagConstraints();
        gbcHero.gridx = 0;
        gbcHero.gridy = 0;
        gbcHero.weightx = 0;
        gbcHero.weighty = 1.0;
        gbcHero.fill = GridBagConstraints.BOTH;
        gbcHero.insets = new Insets(0, 0, 0, 16);

        GridBagConstraints gbcAction = new GridBagConstraints();
        gbcAction.gridx = 1;
        gbcAction.gridy = 0;
        gbcAction.weightx = 1.0;
        gbcAction.weighty = 1.0;
        gbcAction.fill = GridBagConstraints.BOTH;

        ModernCard heroCard = new ModernCard(12, COLOR_HERO_BG, COLOR_BORDER_SUBTLE);
        heroCard.setPreferredSize(new Dimension(390, 560));
        heroCard.setLayout(new BorderLayout(0, 0));
        heroCard.setBorder(new EmptyBorder(22, 22, 20, 22));

        JPanel heroTopSection = new JPanel();
        heroTopSection.setOpaque(false);
        heroTopSection.setLayout(new BoxLayout(heroTopSection, BoxLayout.Y_AXIS));

        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        badgeWrap.setOpaque(false);
        badgeWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        badgeWrap.setMaximumSize(new Dimension(345, 24));

        PillBadge telemetryBadge = new PillBadge("●  ZERO-TRUST TELEMETRY", COLOR_BLUE_LIGHT, COLOR_BLUE_SUBTLE, COLOR_BLUE_BORDER, 6);
        badgeWrap.add(telemetryBadge);
        heroTopSection.add(badgeWrap);
        heroTopSection.add(Box.createVerticalStrut(12));

        JLabel lblHeroHead1 = new JLabel("Enterprise Cloud");
        lblHeroHead1.setFont(FONT_HERO_TITLE);
        lblHeroHead1.setForeground(Color.WHITE);
        lblHeroHead1.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblHeroHead2 = new JLabel("Licensing & Identity");
        lblHeroHead2.setFont(FONT_HERO_TITLE);
        lblHeroHead2.setForeground(COLOR_BLUE_GLOW);
        lblHeroHead2.setAlignmentX(Component.LEFT_ALIGNMENT);

        heroTopSection.add(lblHeroHead1);
        heroTopSection.add(lblHeroHead2);
        heroTopSection.add(Box.createVerticalStrut(8));

        JLabel lblHeroSub = new JLabel("<html><body style='width:330px; color:#A1A1AA; font-size:12px; line-height:17px;'>Cryptographically bound sessions, hardware fingerprinting, real-time cloud variables, and tamper-evident delivery.</body></html>");
        lblHeroSub.setFont(FONT_BODY);
        lblHeroSub.setAlignmentX(Component.LEFT_ALIGNMENT);
        heroTopSection.add(lblHeroSub);
        heroTopSection.add(Box.createVerticalStrut(16));

        String[][] heroFeatures = {
            { "Instant License Key & Account Activation", "Zero-trust session negotiation with anti-replay protection" },
            { "Automatic Device HWID Binding & Reset", "Hardware fingerprint locking with self-service reset support" },
            { "Cloud Variables, Live Chat & File Delivery", "Deliver encrypted configuration and assets on demand" },
            { "Two-Factor (2FA) Account Protection", "Time-based OTP security with instant identity verification" }
        };

        for (String[] feat : heroFeatures) {
            ModernCard featCard = new ModernCard(8, COLOR_INPUT_BG, new Color(30, 36, 52));
            featCard.setLayout(new BorderLayout(10, 0));
            featCard.setBorder(new EmptyBorder(8, 10, 8, 10));
            featCard.setPreferredSize(new Dimension(345, 50));
            featCard.setMaximumSize(new Dimension(345, 50));
            featCard.setAlignmentX(Component.LEFT_ALIGNMENT);

            CheckmarkBadge checkBadge = new CheckmarkBadge();
            JPanel checkWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
            checkWrap.setOpaque(false);
            checkWrap.add(checkBadge);

            JPanel textWrap = new JPanel(new GridLayout(2, 1, 0, 2));
            textWrap.setOpaque(false);

            JLabel lblFTitle = new JLabel(feat[0]);
            lblFTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblFTitle.setForeground(Color.WHITE);

            JLabel lblFSub = new JLabel(feat[1]);
            lblFSub.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            lblFSub.setForeground(COLOR_TEXT_MUTED);

            textWrap.add(lblFTitle);
            textWrap.add(lblFSub);

            featCard.add(checkWrap, BorderLayout.WEST);
            featCard.add(textWrap, BorderLayout.CENTER);

            heroTopSection.add(featCard);
            heroTopSection.add(Box.createVerticalStrut(8));
        }

        heroCard.add(heroTopSection, BorderLayout.NORTH);

        ModernCard boundCard = new ModernCard(8, COLOR_INPUT_BG, new Color(30, 36, 52));
        boundCard.setLayout(new BoxLayout(boundCard, BoxLayout.Y_AXIS));
        boundCard.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel lblBoundHeader = new JLabel("BOUND APPLICATION & DEVICE");
        lblBoundHeader.setFont(new Font("Segoe UI", Font.BOLD, 9));
        lblBoundHeader.setForeground(COLOR_BLUE_GLOW);

        JLabel lblBoundApp = new JLabel((configuredAppName != null && !configuredAppName.isEmpty()) ? configuredAppName : "Unconfigured");
        lblBoundApp.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblBoundApp.setForeground(Color.WHITE);

        JLabel lblBoundHwid = new JLabel("HWID: " + localHwid);
        lblBoundHwid.setFont(FONT_MONO_SM);
        lblBoundHwid.setForeground(COLOR_TEXT_MUTED);

        boundCard.add(lblBoundHeader);
        boundCard.add(Box.createVerticalStrut(3));
        boundCard.add(lblBoundApp);
        boundCard.add(Box.createVerticalStrut(2));
        boundCard.add(lblBoundHwid);

        heroCard.add(boundCard, BorderLayout.SOUTH);
        splitGrid.add(heroCard, gbcHero);

        ModernCard actionCard = new ModernCard(12, COLOR_CARD_BG, COLOR_BORDER_SUBTLE);
        actionCard.setLayout(new BorderLayout(0, 0));
        actionCard.setBorder(new EmptyBorder(20, 28, 20, 28));

        JPanel actionTop = new JPanel();
        actionTop.setOpaque(false);
        actionTop.setLayout(new BoxLayout(actionTop, BoxLayout.Y_AXIS));

        segmentedTabContainer = new JPanel(new GridLayout(1, 5, 4, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(10, 13, 23));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(new Color(28, 33, 51));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        segmentedTabContainer.setOpaque(false);
        segmentedTabContainer.setBorder(new EmptyBorder(3, 3, 3, 3));
        segmentedTabContainer.setPreferredSize(new Dimension(530, 36));
        segmentedTabContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        segmentedTabContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        String[] tabNames = { "Sign In", "Register", "License", "Upgrade", "Reset HWID" };
        AuthMode[] tabModes = {
            AuthMode.LOGIN_CREDENTIALS,
            AuthMode.REGISTER,
            AuthMode.LOGIN_LICENSE,
            AuthMode.UPGRADE,
            AuthMode.RESET_HWID
        };

        for (int i = 0; i < tabNames.length; i++) {
            final AuthMode mode = tabModes[i];
            ModernButton tabBtn = ModernButton.segmentedTab(tabNames[i], i == 0, e -> setAuthMode(mode));
            tabButtons.add(tabBtn);
            segmentedTabContainer.add(tabBtn);
        }

        actionTop.add(segmentedTabContainer);
        actionTop.add(Box.createVerticalStrut(14));

        lblAuthHeading = new JLabel("Sign in to your account");
        lblAuthHeading.setFont(FONT_HEADING);
        lblAuthHeading.setForeground(Color.WHITE);
        lblAuthHeading.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblAuthSubheading = new JLabel("Enter your credentials to negotiate a secure zero-trust session.");
        lblAuthSubheading.setFont(FONT_BODY);
        lblAuthSubheading.setForeground(COLOR_TEXT_DIM);
        lblAuthSubheading.setAlignmentX(Component.LEFT_ALIGNMENT);

        actionTop.add(lblAuthHeading);
        actionTop.add(Box.createVerticalStrut(3));
        actionTop.add(lblAuthSubheading);
        actionTop.add(Box.createVerticalStrut(14));

        actionCard.add(actionTop, BorderLayout.NORTH);

        JPanel formCenter = new JPanel();
        formCenter.setOpaque(false);
        formCenter.setLayout(new BoxLayout(formCenter, BoxLayout.Y_AXIS));

        txtUsername = new ModernTextField(FieldIconType.NONE, "Enter your username");
        panelUsernameGroup = createFieldGroup("USERNAME", txtUsername, null);
        formCenter.add(panelUsernameGroup);

        txtPassword = new ModernPasswordField("Enter your password");
        btnForgotLink = ModernButton.linkButton("Forgot password?", e -> setAuthMode(AuthMode.FORGOT_PASSWORD));
        panelPasswordGroup = createFieldGroup("PASSWORD", txtPassword, btnForgotLink);
        formCenter.add(panelPasswordGroup);

        txt2faCode = new ModernTextField(FieldIconType.NONE, "Enter 6-digit code (if enabled)");
        panel2faGroup = createFieldGroup("2FA CODE (OPTIONAL)", txt2faCode, null);
        formCenter.add(panel2faGroup);

        txtEmail = new ModernTextField(FieldIconType.NONE, "Enter your email");
        panelEmailGroup = createFieldGroup("EMAIL", txtEmail, null);
        formCenter.add(panelEmailGroup);

        txtResetToken = new ModernTextField(FieldIconType.NONE, "Enter the reset token received by email");
        btnForgotToggleToken = ModernButton.linkButton("Already have a reset token?", e -> {
            if (currentAuthMode == AuthMode.FORGOT_PASSWORD) {
                setAuthMode(AuthMode.RESET_PASSWORD_TOKEN);
            } else {
                setAuthMode(AuthMode.FORGOT_PASSWORD);
            }
        });
        panelTokenGroup = createFieldGroup("RESET TOKEN", txtResetToken, btnForgotToggleToken);
        formCenter.add(panelTokenGroup);

        txtLicenseKey = new ModernTextField(FieldIconType.NONE, "Enter your license key");
        txtLicenseKey.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblLicenseHint = new JLabel("<html><body style='color:#6B6B74; font-family:Segoe UI, sans-serif; font-size:10.5px; margin:0; padding:0;'>"
            + "First time using this key creates your account automatically &mdash;<br/>"
            + "no separate registration needed.</body></html>");
        lblLicenseHint.setFont(FONT_BODY_SM);
        lblLicenseHint.setForeground(COLOR_TEXT_MUTED);
        lblLicenseHint.setHorizontalAlignment(SwingConstants.LEFT);
        lblLicenseHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblLicenseHint.setBorder(new EmptyBorder(4, 2, 0, 0));
        lblLicenseHint.setPreferredSize(new Dimension(530, 36));
        lblLicenseHint.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        panelLicenseGroup = new JPanel();
        panelLicenseGroup.setOpaque(false);
        panelLicenseGroup.setLayout(new BoxLayout(panelLicenseGroup, BoxLayout.Y_AXIS));
        panelLicenseGroup.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel lblKeyWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        lblKeyWrap.setOpaque(false);
        lblKeyWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblKeyWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        JLabel lblK = new JLabel("LICENSE KEY");
        lblK.setFont(FONT_LABEL);
        lblK.setForeground(COLOR_TEXT_SEC);
        lblKeyWrap.add(lblK);

        panelLicenseGroup.add(lblKeyWrap);
        panelLicenseGroup.add(Box.createVerticalStrut(4));
        panelLicenseGroup.add(txtLicenseKey);
        panelLicenseGroup.add(lblLicenseHint);
        panelLicenseGroup.add(Box.createVerticalStrut(11));
        panelLicenseGroup.setPreferredSize(new Dimension(530, 115));
        panelLicenseGroup.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        formCenter.add(panelLicenseGroup);

        btnAuthSubmit = ModernButton.primaryButton("Sign In", e -> executeSubmit());
        btnAuthSubmit.setPreferredSize(new Dimension(530, 40));
        btnAuthSubmit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnAuthSubmit.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCenter.add(btnAuthSubmit);

        lblAuthError = new JLabel("", SwingConstants.CENTER);
        lblAuthError.setFont(FONT_BODY);
        lblAuthError.setForeground(COLOR_ERROR);
        lblAuthError.setHorizontalAlignment(SwingConstants.CENTER);
        lblAuthError.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblAuthError.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        lblAuthError.setVisible(false);
        formCenter.add(Box.createVerticalStrut(8));
        formCenter.add(lblAuthError);

        btnSwitchMode = ModernButton.linkButton("Need an account? Register", e -> {
            if (currentAuthMode == AuthMode.LOGIN_CREDENTIALS) {
                setAuthMode(AuthMode.REGISTER);
            } else {
                setAuthMode(AuthMode.LOGIN_CREDENTIALS);
            }
        });
        btnSwitchMode.setHorizontalAlignment(SwingConstants.CENTER);
        btnSwitchMode.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnSwitchMode.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        formCenter.add(Box.createVerticalStrut(10));
        formCenter.add(btnSwitchMode);

        formCenter.add(Box.createVerticalGlue());

        actionCard.add(formCenter, BorderLayout.CENTER);
        splitGrid.add(actionCard, gbcAction);

        outer.add(splitGrid, BorderLayout.CENTER);

        setAuthMode(AuthMode.LOGIN_CREDENTIALS);
        return outer;
    }

    private static JPanel createFieldGroup(String labelText, JComponent field, JComponent trailingLink) {
        JPanel group = new JPanel();
        group.setOpaque(false);
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        headerRow.setPreferredSize(new Dimension(530, 18));

        JLabel l = new JLabel(labelText);
        l.setFont(FONT_LABEL);
        l.setForeground(COLOR_TEXT_SEC);
        headerRow.add(l, BorderLayout.WEST);

        if (trailingLink != null) {
            headerRow.add(trailingLink, BorderLayout.EAST);
        }

        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        group.add(headerRow);
        group.add(Box.createVerticalStrut(4));
        group.add(field);
        group.add(Box.createVerticalStrut(11));
        group.setMaximumSize(new Dimension(Integer.MAX_VALUE, 71));
        return group;
    }

    private void setAuthMode(AuthMode mode) {
        this.currentAuthMode = mode;
        lblAuthError.setVisible(false);
        txt2faCode.setText("");

        int selectedIndex = -1;
        if (mode == AuthMode.LOGIN_CREDENTIALS) selectedIndex = 0;
        else if (mode == AuthMode.REGISTER) selectedIndex = 1;
        else if (mode == AuthMode.LOGIN_LICENSE) selectedIndex = 2;
        else if (mode == AuthMode.UPGRADE) selectedIndex = 3;
        else if (mode == AuthMode.RESET_HWID) selectedIndex = 4;

        segmentedTabContainer.setVisible(selectedIndex >= 0);
        for (int i = 0; i < tabButtons.size(); i++) {
            tabButtons.get(i).setSelectedTab(i == selectedIndex);
        }

        panelUsernameGroup.setVisible(false);
        panelPasswordGroup.setVisible(false);
        panel2faGroup.setVisible(false);
        panelEmailGroup.setVisible(false);
        panelTokenGroup.setVisible(false);
        panelLicenseGroup.setVisible(false);
        lblLicenseHint.setVisible(false);
        btnForgotLink.setVisible(false);
        btnForgotToggleToken.setVisible(false);

        switch (mode) {
            case LOGIN_CREDENTIALS:
                lblAuthHeading.setText("Sign in to your account");
                lblAuthSubheading.setText("Enter your credentials to negotiate a secure zero-trust session.");
                panelUsernameGroup.setVisible(true);
                panelPasswordGroup.setVisible(true);
                panel2faGroup.setVisible(true);
                btnForgotLink.setVisible(true);
                btnAuthSubmit.setText("Sign In");
                btnSwitchMode.setText("Need an account? Register");
                break;

            case LOGIN_LICENSE:
                lblAuthHeading.setText("Instant license access");
                lblAuthSubheading.setText("Authenticate directly using an active license key without username.");
                panelLicenseGroup.setVisible(true);
                lblLicenseHint.setVisible(true);
                btnAuthSubmit.setText("Continue with License");
                btnSwitchMode.setText("Need an account? Register");
                break;

            case REGISTER:
                lblAuthHeading.setText("Create your account");
                lblAuthSubheading.setText("Registers a username bound to an unused license key.");
                panelUsernameGroup.setVisible(true);
                panelPasswordGroup.setVisible(true);
                panelLicenseGroup.setVisible(true);
                panelEmailGroup.setVisible(true);
                btnAuthSubmit.setText("Create Account");
                btnSwitchMode.setText("Already have an account? Sign In");
                break;

            case FORGOT_PASSWORD:
                lblAuthHeading.setText("Forgot password");
                lblAuthSubheading.setText("Enter your email to receive a reset token.");
                panelEmailGroup.setVisible(true);
                btnForgotToggleToken.setText("Already have a reset token?");
                btnForgotToggleToken.setVisible(true);
                btnAuthSubmit.setText("Send Reset Link");
                btnSwitchMode.setText("Back to Sign In");
                break;

            case RESET_PASSWORD_TOKEN:
                lblAuthHeading.setText("Reset password with token");
                lblAuthSubheading.setText("Enter the reset token received by email and your new password.");
                panelTokenGroup.setVisible(true);
                panelPasswordGroup.setVisible(true);
                btnForgotToggleToken.setText("Need a token? Request email");
                btnForgotToggleToken.setVisible(true);
                btnAuthSubmit.setText("Set New Password");
                btnSwitchMode.setText("Back to Sign In");
                break;

            case UPGRADE:
                lblAuthHeading.setText("Upgrade plan with license key");
                lblAuthSubheading.setText("Adds this license's time to the target account's subscription.");
                panelUsernameGroup.setVisible(true);
                panelLicenseGroup.setVisible(true);
                btnAuthSubmit.setText("Apply Upgrade");
                btnSwitchMode.setText("Back to Sign In");
                break;

            case RESET_HWID:
                lblAuthHeading.setText("Reset hardware ID");
                lblAuthSubheading.setText("For when a HWID mismatch blocks login itself.");
                panelUsernameGroup.setVisible(true);
                panelPasswordGroup.setVisible(true);
                panel2faGroup.setVisible(true);
                btnAuthSubmit.setText("Reset HWID");
                btnSwitchMode.setText("Back to Sign In");
                break;
        }

        revalidate();
        repaint();
    }

    private void executeSubmit() {
        lblAuthError.setVisible(false);
        btnAuthSubmit.setEnabled(false);
        btnAuthSubmit.setText("Processing...");

        final String u = txtUsername.getText().trim();
        final String p = txtPassword.getPasswordString().trim();
        final String code = txt2faCode.getText().trim();
        final String email = txtEmail.getText().trim();
        final String token = txtResetToken.getText().trim();
        final String key = txtLicenseKey.getText().trim();

        new Thread(() -> {
            boolean success = false;
            String message = "";
            try {
                switch (currentAuthMode) {
                    case LOGIN_CREDENTIALS:
                        if (u.isEmpty() || p.isEmpty()) {
                            showAuthError("Please enter username and password.");
                            return;
                        }
                        success = api.login(u, p, code.isEmpty() ? null : code);
                        message = api.response.message;
                        break;

                    case LOGIN_LICENSE:
                        if (key.isEmpty()) {
                            showAuthError("Please enter a valid license key.");
                            return;
                        }
                        success = api.license(key);
                        message = api.response.message;
                        break;

                    case REGISTER:
                        if (u.isEmpty() || p.isEmpty() || key.isEmpty()) {
                            showAuthError("Please provide username, password and license key.");
                            return;
                        }
                        success = api.register(u, p, key, email.isEmpty() ? null : email);
                        message = api.response.message;
                        break;

                    case FORGOT_PASSWORD:
                        if (email.isEmpty()) {
                            showAuthError("Please enter your registered email address.");
                            return;
                        }
                        success = api.forgotPassword(email);
                        message = api.response.message != null ? api.response.message : "Reset email dispatched.";
                        break;

                    case RESET_PASSWORD_TOKEN:
                        if (token.isEmpty() || p.isEmpty()) {
                            showAuthError("Please enter reset token and new password.");
                            return;
                        }
                        success = api.resetPassword(token, p);
                        message = api.response.message;
                        break;

                    case UPGRADE:
                        if (u.isEmpty() || key.isEmpty()) {
                            showAuthError("Please enter target username and license key.");
                            return;
                        }
                        success = api.upgrade(u, key);
                        message = api.response.message;
                        break;

                    case RESET_HWID:
                        if (u.isEmpty() || p.isEmpty()) {
                            showAuthError("Please enter username and password.");
                            return;
                        }
                        success = api.resetHwid(u, p, code.isEmpty() ? null : code);
                        message = api.response.message;
                        break;
                }

                final boolean ok = success;
                final String resMsg = message;

                SwingUtilities.invokeLater(() -> {
                    resetSubmitButtonText();
                    if (ok) {
                        if (currentAuthMode == AuthMode.LOGIN_CREDENTIALS ||
                            currentAuthMode == AuthMode.LOGIN_LICENSE ||
                            currentAuthMode == AuthMode.REGISTER) {
                            authenticated = true;
                            transitionToDashboard();
                        } else {
                            updateStatus(resMsg != null ? resMsg : "Action completed successfully.", COLOR_SUCCESS);
                            showAuthSuccess(resMsg != null ? resMsg : "Success!");
                        }
                    } else {
                        showAuthError(resMsg != null ? resMsg : "Authentication failed.");
                    }
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    resetSubmitButtonText();
                    showAuthError("Error: " + ex.getMessage());
                });
            }
        }).start();
    }

    private void resetSubmitButtonText() {
        btnAuthSubmit.setEnabled(true);
        switch (currentAuthMode) {
            case LOGIN_CREDENTIALS: btnAuthSubmit.setText("Sign In"); break;
            case LOGIN_LICENSE: btnAuthSubmit.setText("Continue with License"); break;
            case REGISTER: btnAuthSubmit.setText("Create Account"); break;
            case FORGOT_PASSWORD: btnAuthSubmit.setText("Send Reset Link"); break;
            case RESET_PASSWORD_TOKEN: btnAuthSubmit.setText("Set New Password"); break;
            case UPGRADE: btnAuthSubmit.setText("Apply Upgrade"); break;
            case RESET_HWID: btnAuthSubmit.setText("Reset HWID"); break;
        }
    }

    private void showAuthError(String msg) {
        lblAuthError.setText(msg);
        lblAuthError.setForeground(COLOR_ERROR);
        lblAuthError.setVisible(true);
        btnAuthSubmit.setEnabled(true);
        resetSubmitButtonText();
        updateStatus(msg, COLOR_ERROR);
    }

    private void showAuthSuccess(String msg) {
        lblAuthError.setText(msg);
        lblAuthError.setForeground(COLOR_SUCCESS);
        lblAuthError.setVisible(true);
    }

    private void updateStatus(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            if (lblStatusText != null) {
                lblStatusText.setText(text != null ? text : "");
                lblStatusText.setForeground(color);
            }
        });
    }

    private JPanel buildDashboardView() {
        JPanel dashboard = new JPanel(new BorderLayout());
        dashboard.setOpaque(false);

        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(230, 600));
        sidebar.setBackground(COLOR_CARD_HEADER);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, COLOR_BORDER_SUBTLE));

        JPanel navList = new JPanel();
        navList.setOpaque(false);
        navList.setLayout(new BoxLayout(navList, BoxLayout.Y_AXIS));
        navList.setBorder(new EmptyBorder(18, 12, 12, 12));

        String[] navNames = {
            "01  Overview",
            "02  Subscriptions",
            "03  Variables",
            "04  Live Chat",
            "05  Statistics",
            "06  Security",
            "07  Files & Hooks"
        };
        String[] viewCards = {
            "OVERVIEW",
            "SUBSCRIPTIONS",
            "VARIABLES",
            "CHAT",
            "STATS",
            "SECURITY",
            "FILES_HOOKS"
        };

        for (int i = 0; i < navNames.length; i++) {
            final int idx = i;
            final String cardId = viewCards[i];
            ModernButton navBtn = ModernButton.sidebarNavButton(navNames[i], i == 0, e -> {
                for (int j = 0; j < navButtons.size(); j++) {
                    navButtons.get(j).setSelectedNav(j == idx);
                }
                dashContentLayout.show(dashContentPanel, cardId);
                dashContentPanel.revalidate();
                dashContentPanel.repaint();
                onTabOpened(cardId);
            });
            navButtons.add(navBtn);
            navList.add(navBtn);
            navList.add(Box.createVerticalStrut(4));
        }

        sidebar.add(navList, BorderLayout.CENTER);

        JPanel sideBottom = new JPanel(new BorderLayout());
        sideBottom.setOpaque(false);
        sideBottom.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_BORDER_SUBTLE),
            new EmptyBorder(14, 14, 16, 14)
        ));

        JPanel profileRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        profileRow.setOpaque(false);

        JLabel lblSignedLabel = new JLabel("SIGNED IN AS ");
        lblSignedLabel.setFont(FONT_LABEL);
        lblSignedLabel.setForeground(COLOR_TEXT_MUTED);

        lblSidebarUsername = new JLabel("User");
        lblSidebarUsername.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSidebarUsername.setForeground(Color.WHITE);

        profileRow.add(lblSignedLabel);
        profileRow.add(lblSidebarUsername);

        ModernButton btnLogout = ModernButton.ghostDangerButton("Log Out", e -> performLogout());
        btnLogout.setPreferredSize(new Dimension(200, 32));

        sideBottom.add(profileRow, BorderLayout.NORTH);
        sideBottom.add(Box.createVerticalStrut(10), BorderLayout.CENTER);
        sideBottom.add(btnLogout, BorderLayout.SOUTH);

        sidebar.add(sideBottom, BorderLayout.SOUTH);
        dashboard.add(sidebar, BorderLayout.WEST);

        dashContentPanel.setOpaque(false);

        panelOverview = buildOverviewTab();
        panelSubscriptions = buildSubscriptionsTab();
        panelVariables = buildVariablesTab();
        panelChat = buildChatTab();
        panelStats = buildStatsTab();
        panelSecurity = buildSecurityTab();
        panelFilesHooks = buildFilesHooksTab();

        dashContentPanel.setOpaque(false);
        dashContentPanel.setBorder(new EmptyBorder(28, 32, 28, 32));

        dashContentPanel.add(panelOverview, "OVERVIEW");
        dashContentPanel.add(panelSubscriptions, "SUBSCRIPTIONS");
        dashContentPanel.add(panelVariables, "VARIABLES");
        dashContentPanel.add(panelChat, "CHAT");
        dashContentPanel.add(panelStats, "STATS");
        dashContentPanel.add(panelSecurity, "SECURITY");
        dashContentPanel.add(panelFilesHooks, "FILES_HOOKS");
        dashContentLayout.show(dashContentPanel, "OVERVIEW");

        dashScrollPane = createModernScrollPane(dashContentPanel);
        dashScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        dashScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        dashScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        dashScrollPane.getViewport().setBackground(COLOR_BG_DARK);

        dashboard.add(dashScrollPane, BorderLayout.CENTER);
        return dashboard;
    }

    private void onTabOpened(String tabId) {
        if (dashScrollPane != null) {
            dashScrollPane.getVerticalScrollBar().setValue(0);
            dashScrollPane.getHorizontalScrollBar().setValue(0);
        }
        if ("OVERVIEW".equals(tabId)) {
            refreshOverview();
        } else if ("SUBSCRIPTIONS".equals(tabId)) {
            refreshSubscriptions();
        } else if ("STATS".equals(tabId)) {
            refreshStats();
        }
    }

    private void transitionToDashboard() {
        lblHeaderTitle.setText((configuredAppName != null && !configuredAppName.isEmpty()) ? configuredAppName.toUpperCase() : "NIGHTAUTH");
        lblHeaderSub.setText("CONTROL DASHBOARD");

        String displayUser = (api != null && api.user != null && api.user.username != null && !api.user.username.isEmpty())
            ? api.user.username : "Licensed User";
        lblSidebarUsername.setText(displayUser);

        for (int j = 0; j < navButtons.size(); j++) {
            navButtons.get(j).setSelectedNav(j == 0);
        }
        dashContentLayout.show(dashContentPanel, "OVERVIEW");
        dashContentPanel.revalidate();
        dashContentPanel.repaint();
        if (dashScrollPane != null) {
            dashScrollPane.getVerticalScrollBar().setValue(0);
            dashScrollPane.getHorizontalScrollBar().setValue(0);
        }

        refreshOverview();
        rootCardLayout.show(rootCardPanel, "DASHBOARD");
        updateStatus("Session authenticated. Dashboard active.", COLOR_SUCCESS);
    }

    private void performLogout() {
        new Thread(() -> {
            try {
                if (api != null) api.logout();
            } catch (Exception ignored) {}
            authenticated = false;
            SwingUtilities.invokeLater(() -> {
                txtUsername.setText("");
                txtPassword.setText("");
                txt2faCode.setText("");
                txtLicenseKey.setText("");
                txtEmail.setText("");
                lblHeaderTitle.setText("NIGHTAUTH");
                lblHeaderSub.setText("NATIVE JAVA / SECURITY CLIENT");
                rootCardLayout.show(rootCardPanel, "AUTH");
                setAuthMode(AuthMode.LOGIN_CREDENTIALS);
                updateStatus("Logged out successfully.", COLOR_TEXT_MUTED);
            });
        }).start();
    }

    private JPanel buildOverviewTab() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel("Overview");
        lblTitle.setFont(FONT_DASH_TITLE);
        lblTitle.setForeground(COLOR_TEXT_HEAD);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSub = new JLabel("Account information and active subscription details.");
        lblSub.setFont(FONT_BODY);
        lblSub.setForeground(COLOR_TEXT_MUTED);
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(lblTitle);
        p.add(Box.createVerticalStrut(4));
        p.add(lblSub);
        p.add(Box.createVerticalStrut(20));

        ModernCard userCard = new ModernCard(12, COLOR_CARD_BG, COLOR_BORDER_SUBTLE);
        userCard.setLayout(new GridLayout(2, 2, 20, 16));
        userCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        userCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        userCard.setPreferredSize(new Dimension(680, 130));
        userCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));

        userCard.setName("UserCard");
        p.add(userCard);
        p.add(Box.createVerticalStrut(20));

        JPanel secSubHeader = createSectionHeader("Active Subscriptions & Entitlements");
        secSubHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        secSubHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        p.add(secSubHeader);
        p.add(Box.createVerticalStrut(12));

        JPanel subsContainer = new JPanel();
        subsContainer.setOpaque(false);
        subsContainer.setLayout(new BoxLayout(subsContainer, BoxLayout.Y_AXIS));
        subsContainer.setName("SubsContainer");
        subsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        subsContainer.add(createEmptySubscriptionsCard());
        p.add(subsContainer);
        p.add(Box.createVerticalStrut(20));

        JPanel secInsightHeader = createSectionHeader("Application Live Insights");
        secInsightHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        secInsightHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        p.add(secInsightHeader);
        p.add(Box.createVerticalStrut(12));

        ModernCard bentoCard = new ModernCard(12, COLOR_CARD_BG, COLOR_BORDER_SUBTLE);
        bentoCard.setLayout(new GridLayout(1, 3, 20, 0));
        bentoCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        bentoCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        bentoCard.setPreferredSize(new Dimension(680, 95));
        bentoCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));
        bentoCard.setName("BentoCard");

        bentoCard.add(createBentoCol("TOTAL USERS", "—", Color.WHITE));
        bentoCard.add(createBentoCol("TOTAL LICENSES", "—", Color.WHITE));
        bentoCard.add(createBentoCol("ONLINE NOW", "—", COLOR_SUCCESS));

        p.add(bentoCard);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private void refreshOverview() {
        if (panelOverview == null || api == null) return;

        for (Component c : panelOverview.getComponents()) {
            if ("UserCard".equals(c.getName()) && c instanceof JPanel) {
                JPanel uc = (JPanel) c;
                uc.removeAll();

                String email = (api.user != null && api.user.email != null && !api.user.email.isEmpty()) ? api.user.email : "Not linked";
                String ip = (api.user != null && api.user.ip != null && !api.user.ip.isEmpty()) ? api.user.ip : "—";
                String created = (api.user != null) ? formatTimestamp(api.user.createdAt) : "—";
                String lastLogin = (api.user != null) ? formatTimestamp(api.user.lastLoginAt) : "—";

                uc.add(createTile("EMAIL ADDRESS", email, false));
                uc.add(createTile("ACCOUNT CREATED", created, false));
                uc.add(createTile("IP ADDRESS", ip, true));
                uc.add(createTile("LAST LOGIN (UTC)", lastLogin, false));
                uc.revalidate();
                uc.repaint();
            } else if ("SubsContainer".equals(c.getName()) && c instanceof JPanel) {
                JPanel sc = (JPanel) c;
                sc.removeAll();

                if (api.user == null || api.user.subscriptions == null || api.user.subscriptions.isEmpty()) {
                    sc.add(createEmptySubscriptionsCard());
                } else {
                    for (NightAuthApi.SubscriptionData sub : api.user.subscriptions) {
                        sc.add(createSubscriptionItemCard(sub));
                        sc.add(Box.createVerticalStrut(8));
                    }
                }
                sc.revalidate();
                sc.repaint();
            } else if ("BentoCard".equals(c.getName()) && c instanceof JPanel) {
                JPanel bc = (JPanel) c;
                new Thread(() -> {
                    long totalUsers = 0;
                    long totalLicenses = 0;
                    int onlineUsers = 0;

                    NightAuthApi.AppStats s = api.getStats();
                    if (s != null) {
                        totalUsers = s.totalUsers;
                        totalLicenses = s.totalLicenses;
                    }
                    List<String> o = api.getOnlineUsers();
                    if (o != null) {
                        onlineUsers = o.size();
                    }

                    final long fUsers = totalUsers;
                    final long fLicenses = totalLicenses;
                    final int fOnline = onlineUsers;

                    SwingUtilities.invokeLater(() -> {
                        bc.removeAll();
                        bc.add(createBentoCol("TOTAL USERS", String.valueOf(fUsers), Color.WHITE));
                        bc.add(createBentoCol("TOTAL LICENSES", String.valueOf(fLicenses), Color.WHITE));
                        bc.add(createBentoCol("ONLINE NOW", String.valueOf(fOnline), COLOR_SUCCESS));
                        bc.revalidate();
                        bc.repaint();
                        if (panelOverview != null) {
                            panelOverview.revalidate();
                            panelOverview.repaint();
                        }
                    });
                }).start();
            }
        }

        panelOverview.revalidate();
        panelOverview.repaint();
        if (dashContentPanel != null) {
            dashContentPanel.revalidate();
            dashContentPanel.repaint();
        }
    }

    private JPanel createTile(String label, String value, boolean isMono) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 4));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(FONT_LABEL);
        l.setForeground(COLOR_TEXT_SEC);

        JLabel v = new JLabel(value);
        v.setFont(isMono ? FONT_MONO : new Font("Segoe UI", Font.BOLD, 13));
        v.setForeground(isMono ? COLOR_TEXT_SEC : COLOR_TEXT_HEAD);

        p.add(l);
        p.add(v);
        return p;
    }

    private JPanel createBentoCol(String label, String value, Color valColor) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 4));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(FONT_LABEL);
        l.setForeground(COLOR_TEXT_MUTED);

        JLabel v = new JLabel(value);
        v.setFont(new Font("Segoe UI", Font.BOLD, 24));
        v.setForeground(valColor);

        p.add(l);
        p.add(v);
        return p;
    }

    private JPanel createSubscriptionItemCard(NightAuthApi.SubscriptionData sub) {
        ModernCard card = new ModernCard(8, COLOR_INPUT_BG, COLOR_INPUT_BORDER);
        card.setLayout(new BorderLayout(14, 0));
        card.setBorder(new EmptyBorder(10, 14, 10, 14));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setPreferredSize(new Dimension(680, 62));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftPanel.setOpaque(false);

        JPanel stripe = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_SUCCESS);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 3, 3);
                g2.dispose();
            }
        };
        stripe.setOpaque(false);
        stripe.setPreferredSize(new Dimension(3, 36));

        JPanel textBlock = new JPanel();
        textBlock.setOpaque(false);
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));

        String planName = (sub != null && sub.subscription != null && !sub.subscription.isEmpty())
            ? sub.subscription : "Default Plan";
        JLabel lblPlan = new JLabel(planName);
        lblPlan.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPlan.setForeground(COLOR_TEXT_HEAD);

        String keyVal = (sub != null && sub.key != null && !sub.key.isEmpty()) ? sub.key : "Direct Entitlement";
        JLabel lblKey = new JLabel("Key: " + keyVal);
        lblKey.setFont(FONT_MONO);
        lblKey.setForeground(COLOR_TEXT_MUTED);

        textBlock.add(lblPlan);
        textBlock.add(Box.createVerticalStrut(3));
        textBlock.add(lblKey);

        leftPanel.add(stripe);
        leftPanel.add(textBlock);

        JPanel rightPanel = new JPanel();
        rightPanel.setOpaque(false);
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

        String timeRem = (sub != null) ? formatTimeRemaining(sub.timeLeft) : "";
        String badgeText;
        if (timeRem != null && !timeRem.isEmpty() && !timeRem.equals("0s") && !timeRem.equals("Expired")) {
            badgeText = timeRem + " remaining";
        } else if (sub != null && sub.expiry != null && !sub.expiry.isEmpty()) {
            badgeText = "EXPIRES: " + formatTimestamp(sub.expiry);
        } else {
            badgeText = "EXPIRES: LIFETIME";
        }

        PillBadge badge = new PillBadge(badgeText, new Color(52, 211, 153), new Color(14, 46, 38), new Color(24, 92, 70), 6);
        badge.setAlignmentX(Component.RIGHT_ALIGNMENT);
        rightPanel.add(badge);

        if (sub != null && sub.expiry != null && !sub.expiry.isEmpty()) {
            JLabel lblExp = new JLabel("Expires: " + formatTimestamp(sub.expiry));
            lblExp.setFont(FONT_LABEL);
            lblExp.setForeground(COLOR_TEXT_MUTED);
            lblExp.setAlignmentX(Component.RIGHT_ALIGNMENT);
            rightPanel.add(Box.createVerticalStrut(3));
            rightPanel.add(lblExp);
        }

        card.add(leftPanel, BorderLayout.WEST);
        card.add(rightPanel, BorderLayout.EAST);
        return card;
    }

    private JPanel createEmptySubscriptionsCard() {
        ModernCard emptyCard = new ModernCard(8, COLOR_INPUT_BG, COLOR_BORDER_SUBTLE);
        emptyCard.setLayout(new BoxLayout(emptyCard, BoxLayout.Y_AXIS));
        emptyCard.setBorder(new EmptyBorder(16, 18, 16, 18));
        emptyCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        emptyCard.setPreferredSize(new Dimension(680, 76));
        emptyCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));

        JLabel lblEmpty = new JLabel("No Active Subscription Tiers");
        lblEmpty.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblEmpty.setForeground(COLOR_TEXT_HEAD);
        lblEmpty.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblEmptySub = new JLabel("This account does not currently have any recurring or lifetime subscription entitlements attached.");
        lblEmptySub.setFont(FONT_BODY_SM);
        lblEmptySub.setForeground(COLOR_TEXT_MUTED);
        lblEmptySub.setAlignmentX(Component.LEFT_ALIGNMENT);

        emptyCard.add(lblEmpty);
        emptyCard.add(Box.createVerticalStrut(4));
        emptyCard.add(lblEmptySub);
        return emptyCard;
    }

    private JPanel buildSubscriptionsTab() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel("Subscriptions");
        lblTitle.setFont(FONT_DASH_TITLE);
        lblTitle.setForeground(COLOR_TEXT_HEAD);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSub = new JLabel("Active subscription licenses and expiration information.");
        lblSub.setFont(FONT_BODY);
        lblSub.setForeground(COLOR_TEXT_MUTED);
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(lblTitle);
        p.add(Box.createVerticalStrut(4));
        p.add(lblSub);
        p.add(Box.createVerticalStrut(20));

        ModernCard card = new ModernCard(12, COLOR_CARD_BG, COLOR_BORDER_SUBTLE);
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(new EmptyBorder(18, 20, 18, 20));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setPreferredSize(new Dimension(680, 360));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JPanel titleWithBadge = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleWithBadge.setOpaque(false);
        titleWithBadge.add(createSectionHeader("ACTIVE SUBSCRIPTIONS & ENTITLEMENTS"));

        badgeActiveSubsCount = new PillBadge("0 ACTIVE", COLOR_SUCCESS, new Color(16, 185, 129, 28), new Color(16, 185, 129, 56), 6);
        titleWithBadge.add(badgeActiveSubsCount);
        headerRow.add(titleWithBadge, BorderLayout.WEST);

        ModernButton btnRefresh = ModernButton.ghostButton("Refresh Subscriptions", e -> refreshSubscriptions());
        headerRow.add(btnRefresh, BorderLayout.EAST);
        card.add(headerRow, BorderLayout.NORTH);

        panelSubsList = new JPanel();
        panelSubsList.setOpaque(false);
        panelSubsList.setLayout(new BoxLayout(panelSubsList, BoxLayout.Y_AXIS));
        panelSubsList.add(createEmptySubscriptionsCard());

        JScrollPane scrollSubs = createModernScrollPane(panelSubsList);
        scrollSubs.setPreferredSize(new Dimension(680, 270));
        scrollSubs.setBorder(null);
        card.add(scrollSubs, BorderLayout.CENTER);

        card.setName("SubCard");
        p.add(card);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private void refreshSubscriptions() {
        if (panelSubscriptions == null || api == null) return;
        new Thread(() -> {
            try {
                api.check();
            } catch (Exception ignored) {}

            SwingUtilities.invokeLater(() -> {
                if (panelSubsList != null) {
                    panelSubsList.removeAll();
                    int count = (api.user != null && api.user.subscriptions != null) ? api.user.subscriptions.size() : 0;
                    if (badgeActiveSubsCount != null) {
                        badgeActiveSubsCount.setText(count + " ACTIVE");
                    }
                    if (count == 0) {
                        panelSubsList.add(createEmptySubscriptionsCard());
                    } else {
                        for (NightAuthApi.SubscriptionData sub : api.user.subscriptions) {
                            panelSubsList.add(createSubscriptionItemCard(sub));
                            panelSubsList.add(Box.createVerticalStrut(8));
                        }
                    }
                    panelSubsList.revalidate();
                    panelSubsList.repaint();
                }
                updateStatus("Subscriptions refreshed.", COLOR_SUCCESS);
            });
        }).start();
    }

    private JPanel buildVariablesTab() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel("Variables");
        lblTitle.setFont(FONT_DASH_TITLE);
        lblTitle.setForeground(COLOR_TEXT_HEAD);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSub = new JLabel("Inspect global cloud runtime variables or manage user-scoped personal storage.");
        lblSub.setFont(FONT_BODY);
        lblSub.setForeground(COLOR_TEXT_MUTED);
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(lblTitle);
        p.add(Box.createVerticalStrut(4));
        p.add(lblSub);
        p.add(Box.createVerticalStrut(18));

        ModernCard cardGlobal = new ModernCard(12, COLOR_CARD_BG, COLOR_BORDER_SUBTLE);
        cardGlobal.setLayout(new BorderLayout(0, 12));
        cardGlobal.setBorder(new EmptyBorder(16, 18, 16, 18));
        cardGlobal.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardGlobal.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));

        cardGlobal.add(createSectionHeaderWithSub("Global Variables", "•  App-wide constants", COLOR_ACCENT_BLUE), BorderLayout.NORTH);

        JPanel rowGlobal = new JPanel(new GridBagLayout());
        rowGlobal.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 0, 8);

        ModernTextField txtGlobKey = new ModernTextField(FieldIconType.NONE, "Variable key (e.g. motd, api_url)");
        txtGlobKey.setPreferredSize(new Dimension(220, 36));
        g.gridx = 0; g.weightx = 0.35;
        rowGlobal.add(txtGlobKey, g);

        ModernTextField txtGlobVal = new ModernTextField(FieldIconType.NONE, "Returned value will appear here...");
        txtGlobVal.setEditable(false);
        txtGlobVal.setPreferredSize(new Dimension(280, 36));
        g.gridx = 1; g.weightx = 0.45;
        rowGlobal.add(txtGlobVal, g);

        ModernButton btnFetchGlob = ModernButton.primaryButton("Fetch Value", e -> {
            String k = txtGlobKey.getText().trim();
            if (k.isEmpty()) return;
            new Thread(() -> {
                String val = api.getGlobalVar(k);
                SwingUtilities.invokeLater(() -> {
                    txtGlobVal.setText(val != null && !val.isEmpty() ? val : "(not found)");
                    updateStatus("Variable fetched.", COLOR_SUCCESS);
                });
            }).start();
        });
        btnFetchGlob.setPreferredSize(new Dimension(120, 36));
        g.gridx = 2; g.weightx = 0.2; g.insets = new Insets(0, 0, 0, 0);
        rowGlobal.add(btnFetchGlob, g);

        cardGlobal.add(rowGlobal, BorderLayout.CENTER);
        p.add(cardGlobal);
        p.add(Box.createVerticalStrut(16));

        ModernCard cardUser = new ModernCard(12, COLOR_CARD_BG, COLOR_BORDER_SUBTLE);
        cardUser.setLayout(new BorderLayout(0, 12));
        cardUser.setBorder(new EmptyBorder(16, 18, 16, 18));
        cardUser.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardUser.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));

        cardUser.add(createSectionHeaderWithSub("User Variables", "•  Personal cloud storage bound to your account", COLOR_ACCENT_PURPLE), BorderLayout.NORTH);

        JPanel rowUser = new JPanel(new GridBagLayout());
        rowUser.setOpaque(false);
        g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 0, 8);

        ModernTextField txtUserKey = new ModernTextField(FieldIconType.NONE, "Key name");
        txtUserKey.setPreferredSize(new Dimension(170, 36));
        g.gridx = 0; g.weightx = 0.25;
        rowUser.add(txtUserKey, g);

        ModernTextField txtUserVal = new ModernTextField(FieldIconType.NONE, "Custom value to save or view");
        txtUserVal.setPreferredSize(new Dimension(230, 36));
        g.gridx = 1; g.weightx = 0.45;
        rowUser.add(txtUserVal, g);

        ModernButton btnGetUser = ModernButton.ghostButton("Get Value", e -> {
            String k = txtUserKey.getText().trim();
            if (k.isEmpty()) return;
            new Thread(() -> {
                String val = api.getUserVar(k);
                SwingUtilities.invokeLater(() -> {
                    txtUserVal.setText(val != null ? val : "");
                    updateStatus("User variable fetched.", COLOR_SUCCESS);
                });
            }).start();
        });
        btnGetUser.setPreferredSize(new Dimension(100, 36));
        g.gridx = 2; g.weightx = 0.15;
        rowUser.add(btnGetUser, g);

        ModernButton btnSetUser = ModernButton.primaryButton("Save Value", e -> {
            String k = txtUserKey.getText().trim();
            String v = txtUserVal.getText().trim();
            if (k.isEmpty()) return;
            new Thread(() -> {
                boolean ok = api.setUserVar(k, v);
                SwingUtilities.invokeLater(() -> {
                    updateStatus(ok ? "User variable saved." : "Failed to save user variable.", ok ? COLOR_SUCCESS : COLOR_ERROR);
                });
            }).start();
        });
        btnSetUser.setPreferredSize(new Dimension(105, 36));
        g.gridx = 3; g.weightx = 0.15; g.insets = new Insets(0, 0, 0, 0);
        rowUser.add(btnSetUser, g);

        cardUser.add(rowUser, BorderLayout.CENTER);
        p.add(cardUser);
        p.add(Box.createVerticalStrut(16));

        ModernCard cardAll = new ModernCard(12, COLOR_CARD_BG, COLOR_BORDER_SUBTLE);
        cardAll.setLayout(new BorderLayout(0, 12));
        cardAll.setBorder(new EmptyBorder(16, 18, 16, 18));
        cardAll.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardAll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        JPanel allHeader = new JPanel(new BorderLayout());
        allHeader.setOpaque(false);
        allHeader.add(createSectionHeader("All User Cloud Variables"), BorderLayout.WEST);

        panelUserVarsList = new JPanel();
        panelUserVarsList.setOpaque(false);
        panelUserVarsList.setLayout(new BoxLayout(panelUserVarsList, BoxLayout.Y_AXIS));

        JLabel lblEmptyVars = new JLabel("Click 'Fetch All Variables' to inspect all key-value pairs stored for this account.");
        lblEmptyVars.setFont(FONT_BODY);
        lblEmptyVars.setForeground(COLOR_TEXT_MUTED);
        panelUserVarsList.add(lblEmptyVars);

        ModernButton btnFetchAll = ModernButton.ghostButton("Fetch All Variables", e -> {
            new Thread(() -> {
                boolean ok = api.getUserVariables();
                SwingUtilities.invokeLater(() -> {
                    if (panelUserVarsList != null) {
                        panelUserVarsList.removeAll();
                        boolean hasItems = false;
                        if (ok && api.response != null && api.response.payload != null) {
                            Object varsObj = api.response.payload.get("variables");
                            if (varsObj instanceof List) {
                                for (Object item : (List<?>) varsObj) {
                                    if (item instanceof Map) {
                                        Map<?, ?> m = (Map<?, ?>) item;
                                        String k = m.get("key") != null ? String.valueOf(m.get("key")) : "";
                                        String v = m.get("value") != null ? String.valueOf(m.get("value")) : "";
                                        panelUserVarsList.add(createVariableRowCard(k, v));
                                        panelUserVarsList.add(Box.createVerticalStrut(6));
                                        hasItems = true;
                                    }
                                }
                            }
                        }
                        if (!hasItems) {
                            JLabel lblNone = new JLabel("No user cloud variables currently registered.");
                            lblNone.setFont(FONT_BODY);
                            lblNone.setForeground(COLOR_TEXT_MUTED);
                            panelUserVarsList.add(lblNone);
                        }
                        panelUserVarsList.revalidate();
                        panelUserVarsList.repaint();
                    }
                    updateStatus("All user variables refreshed.", COLOR_SUCCESS);
                });
            }).start();
        });
        allHeader.add(btnFetchAll, BorderLayout.EAST);
        cardAll.add(allHeader, BorderLayout.NORTH);

        JScrollPane scrollAll = createModernScrollPane(panelUserVarsList);
        scrollAll.setPreferredSize(new Dimension(680, 160));
        scrollAll.setBorder(null);
        cardAll.add(scrollAll, BorderLayout.CENTER);

        p.add(cardAll);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JPanel createVariableRowCard(String key, String value) {
        ModernCard row = new ModernCard(6, COLOR_INPUT_BG, new Color(30, 36, 50));
        row.setLayout(new BorderLayout(12, 0));
        row.setBorder(new EmptyBorder(6, 14, 6, 14));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setPreferredSize(new Dimension(680, 34));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JLabel lblContent = new JLabel(key + "  =  " + value);
        lblContent.setFont(FONT_MONO);
        lblContent.setForeground(COLOR_BLUE_LIGHT);
        row.add(lblContent, BorderLayout.CENTER);
        return row;
    }

    private JPanel buildChatTab() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel("Live Chat");
        lblTitle.setFont(FONT_DASH_TITLE);
        lblTitle.setForeground(COLOR_TEXT_HEAD);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSub = new JLabel("Real-time communication channel for this application.");
        lblSub.setFont(FONT_BODY);
        lblSub.setForeground(COLOR_TEXT_MUTED);
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(lblTitle);
        p.add(Box.createVerticalStrut(4));
        p.add(lblSub);
        p.add(Box.createVerticalStrut(14));

        ModernCard connCard = new ModernCard(10, COLOR_CARD_BG, COLOR_BORDER_SUBTLE);
        connCard.setLayout(new BorderLayout(10, 0));
        connCard.setBorder(new EmptyBorder(10, 12, 10, 12));
        connCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        connCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        txtChatRoom = new ModernTextField(FieldIconType.NONE, "Enter room name (e.g. general, announcements)");
        txtChatRoom.setText("general");
        connCard.add(txtChatRoom, BorderLayout.CENTER);

        JPanel connActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        connActions.setOpaque(false);

        ModernButton btnConnectRoom = ModernButton.primaryButton("Connect Room", e -> connectChatRoom());
        btnConnectRoom.setPreferredSize(new Dimension(130, 36));

        badgeRoomStatus = new PillBadge("● Offline", COLOR_TEXT_MUTED, new Color(255, 255, 255, 12), COLOR_BORDER_SUBTLE, 7);

        connActions.add(btnConnectRoom);
        connActions.add(badgeRoomStatus);
        connCard.add(connActions, BorderLayout.EAST);

        p.add(connCard);
        p.add(Box.createVerticalStrut(12));

        ModernCard messagesCard = new ModernCard(12, COLOR_CARD_BG, COLOR_BORDER_SUBTLE);
        messagesCard.setLayout(new BorderLayout());
        messagesCard.setBorder(new EmptyBorder(12, 12, 12, 12));
        messagesCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        messagesCard.setPreferredSize(new Dimension(680, 310));
        messagesCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 310));

        chatMessagesContainer = new JPanel();
        chatMessagesContainer.setOpaque(false);
        chatMessagesContainer.setLayout(new BoxLayout(chatMessagesContainer, BoxLayout.Y_AXIS));

        chatEmptyPlaceholder = new JPanel();
        chatEmptyPlaceholder.setOpaque(false);
        chatEmptyPlaceholder.setLayout(new BoxLayout(chatEmptyPlaceholder, BoxLayout.Y_AXIS));
        chatEmptyPlaceholder.setBorder(new EmptyBorder(50, 0, 50, 0));

        JLabel lblNoMsg = new JLabel("No messages loaded in this channel yet", SwingConstants.CENTER);
        lblNoMsg.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblNoMsg.setForeground(COLOR_TEXT_SEC);
        lblNoMsg.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblNoMsgSub = new JLabel("Click 'Connect Room' or send a message below to start chatting.", SwingConstants.CENTER);
        lblNoMsgSub.setFont(FONT_BODY_SM);
        lblNoMsgSub.setForeground(COLOR_TEXT_MUTED);
        lblNoMsgSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        chatEmptyPlaceholder.add(lblNoMsg);
        chatEmptyPlaceholder.add(Box.createVerticalStrut(4));
        chatEmptyPlaceholder.add(lblNoMsgSub);

        chatMessagesContainer.add(chatEmptyPlaceholder);

        scrollChatMessages = createModernScrollPane(chatMessagesContainer);
        scrollChatMessages.getViewport().setBackground(COLOR_CARD_BG);
        messagesCard.add(scrollChatMessages, BorderLayout.CENTER);
        p.add(messagesCard);
        p.add(Box.createVerticalStrut(12));

        ModernCard inputCard = new ModernCard(10, COLOR_CARD_BG, COLOR_BORDER_SUBTLE);
        inputCard.setLayout(new BorderLayout(10, 0));
        inputCard.setBorder(new EmptyBorder(8, 8, 8, 8));
        inputCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        txtChatMessage = new ModernTextField(FieldIconType.NONE, "Type your message and press Enter...");
        txtChatMessage.addActionListener(e -> sendChatMessage());
        inputCard.add(txtChatMessage, BorderLayout.CENTER);

        ModernButton btnSend = ModernButton.primaryButton("Send", e -> sendChatMessage());
        btnSend.setPreferredSize(new Dimension(80, 36));
        inputCard.add(btnSend, BorderLayout.EAST);

        p.add(inputCard);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private void connectChatRoom() {
        String ch = txtChatRoom.getText().trim();
        if (ch.isEmpty()) return;
        currentChatChannel = ch;

        new Thread(() -> {
            List<NightAuthApi.ChatMessage> list = api.getChatMessages(ch);
            boolean ok = api.response.success;
            SwingUtilities.invokeLater(() -> {
                if (ok) {
                    badgeRoomStatus.setText("● Connected: #" + ch);
                    badgeRoomStatus.setTextAndPillColors(COLOR_SUCCESS, new Color(16, 185, 129, 32), new Color(16, 185, 129, 64));
                    if (badgeRoomStatus.getParent() != null) {
                        badgeRoomStatus.getParent().revalidate();
                        badgeRoomStatus.getParent().repaint();
                        if (badgeRoomStatus.getParent().getParent() != null) {
                            badgeRoomStatus.getParent().getParent().revalidate();
                            badgeRoomStatus.getParent().getParent().repaint();
                        }
                    }
                    chatMessagesContainer.removeAll();
                    updateStatus("Connected to chat channel #" + ch, COLOR_SUCCESS);

                    if (list != null && !list.isEmpty()) {
                        for (NightAuthApi.ChatMessage m : list) {
                            addChatMessageBubble(m.author, m.message, m.timestamp);
                        }
                    } else {
                        chatMessagesContainer.add(chatEmptyPlaceholder);
                    }
                    chatMessagesContainer.revalidate();
                    chatMessagesContainer.repaint();
                } else {
                    badgeRoomStatus.setText("● Connection Error");
                    badgeRoomStatus.setTextAndPillColors(COLOR_ERROR, new Color(244, 63, 94, 32), new Color(244, 63, 94, 64));
                    if (badgeRoomStatus.getParent() != null) {
                        badgeRoomStatus.getParent().revalidate();
                        badgeRoomStatus.getParent().repaint();
                        if (badgeRoomStatus.getParent().getParent() != null) {
                            badgeRoomStatus.getParent().getParent().revalidate();
                            badgeRoomStatus.getParent().getParent().repaint();
                        }
                    }
                    updateStatus("Failed to join room: " + api.response.message, COLOR_ERROR);
                }
            });
        }).start();
    }

    private void sendChatMessage() {
        if (currentChatChannel == null) {
            connectChatRoom();
            return;
        }
        String msg = txtChatMessage.getText().trim();
        if (msg.isEmpty()) return;
        txtChatMessage.setText("");

        new Thread(() -> {
            boolean ok = api.sendChatMessage(msg, currentChatChannel);
            SwingUtilities.invokeLater(() -> {
                if (ok) {
                    String author = (api.user != null && api.user.username != null) ? api.user.username : "You";
                    addChatMessageBubble(author, msg, String.valueOf(Instant.now().getEpochSecond()));
                    updateStatus("Message dispatched.", COLOR_SUCCESS);
                } else {
                    updateStatus("Send failed: " + api.response.message, COLOR_ERROR);
                }
            });
        }).start();
    }

    private void addChatMessageBubble(String author, String message, String timestamp) {
        if (chatMessagesContainer == null) return;
        chatEmptyPlaceholder.setVisible(false);

        String myUsername = (api != null && api.user != null && api.user.username != null) ? api.user.username : "";
        boolean isMe = myUsername.equalsIgnoreCase(author) || "You".equalsIgnoreCase(author);

        JPanel row = new JPanel(new FlowLayout(isMe ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 4));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        ModernCard bubble = new ModernCard(10,
            isMe ? new Color(59, 130, 246, 50) : new Color(255, 255, 255, 18),
            isMe ? new Color(59, 130, 246, 85) : new Color(255, 255, 255, 24)
        );
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(new EmptyBorder(8, 12, 8, 12));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblAuthor = new JLabel(isMe ? "You" : author);
        lblAuthor.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblAuthor.setForeground(isMe ? COLOR_BLUE_GLOW : COLOR_TEXT_SEC);

        JLabel lblTime = new JLabel(formatTimestamp(timestamp));
        lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblTime.setForeground(COLOR_TEXT_MUTED);

        header.add(lblAuthor);
        header.add(lblTime);

        JLabel lblContent = new JLabel("<html><body style='margin:0; padding:0; text-align:left; max-width:440px; color:#F4F4F5; font-size:12px;'>" + escapeHtml(message) + "</body></html>");
        lblContent.setFont(FONT_BODY);
        lblContent.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblContent.setHorizontalAlignment(SwingConstants.LEFT);

        bubble.add(header);
        bubble.add(Box.createVerticalStrut(3));
        bubble.add(lblContent);

        row.add(bubble);
        chatMessagesContainer.add(row);
        chatMessagesContainer.revalidate();

        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollChatMessages.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private JLabel lblStatTotalUsers;
    private JLabel lblStatOnlineUsers;
    private JLabel lblStatTotalKeys;
    private JLabel lblStatVersion;
    private DefaultListModel<String> onlineUsersListModel;

    private JPanel buildStatsTab() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel("App Statistics");
        lblTitle.setFont(FONT_DASH_TITLE);
        lblTitle.setForeground(COLOR_TEXT_HEAD);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSub = new JLabel("Telemetry, active connections, and server ecosystem metrics.");
        lblSub.setFont(FONT_BODY);
        lblSub.setForeground(COLOR_TEXT_MUTED);
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(lblTitle);
        p.add(Box.createVerticalStrut(4));
        p.add(lblSub);
        p.add(Box.createVerticalStrut(20));

        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 12, 0));
        kpiRow.setOpaque(false);
        kpiRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        kpiRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85));

        lblStatTotalUsers = new JLabel("—");
        lblStatOnlineUsers = new JLabel("—");
        lblStatTotalKeys = new JLabel("—");
        lblStatVersion = new JLabel(configuredAppVer);

        kpiRow.add(createKpiCard("TOTAL USERS", lblStatTotalUsers, COLOR_ACCENT_BLUE));
        kpiRow.add(createKpiCard("ONLINE USERS", lblStatOnlineUsers, COLOR_SUCCESS));
        kpiRow.add(createKpiCard("TOTAL LICENSES", lblStatTotalKeys, Color.WHITE));
        kpiRow.add(createKpiCard("APP VERSION", lblStatVersion, COLOR_ACCENT_PURPLE));

        p.add(kpiRow);
        p.add(Box.createVerticalStrut(20));

        ModernCard sessionsCard = new ModernCard(12, COLOR_CARD_BG, COLOR_BORDER_SUBTLE);
        sessionsCard.setLayout(new BorderLayout(0, 14));
        sessionsCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        sessionsCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        sessionsCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(createSectionHeader("Active User Sessions"), BorderLayout.WEST);

        ModernButton btnRefStats = ModernButton.ghostButton("Refresh Statistics", e -> refreshStats());
        head.add(btnRefStats, BorderLayout.EAST);
        sessionsCard.add(head, BorderLayout.NORTH);

        onlineUsersListModel = new DefaultListModel<>();
        JList<String> list = new JList<>(onlineUsersListModel);
        list.setBackground(new Color(10, 10, 12));
        list.setCellRenderer(new SessionListCellRenderer());

        JScrollPane scrollList = createModernScrollPane(list);
        scrollList.setPreferredSize(new Dimension(680, 220));
        sessionsCard.add(scrollList, BorderLayout.CENTER);

        p.add(sessionsCard);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JPanel createKpiCard(String label, JLabel valueLabel, Color valColor) {
        ModernCard c = new ModernCard(10, COLOR_CARD_BG, COLOR_BORDER_SUBTLE);
        c.setLayout(new GridLayout(2, 1, 0, 4));
        c.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel l = new JLabel(label);
        l.setFont(FONT_LABEL);
        l.setForeground(COLOR_TEXT_SEC);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valueLabel.setForeground(valColor);

        c.add(l);
        c.add(valueLabel);
        return c;
    }

    private void refreshStats() {
        if (lblStatTotalUsers == null || api == null) return;
        new Thread(() -> {
            NightAuthApi.AppStats s = api.getStats();
            List<String> o = api.getOnlineUsers();

            SwingUtilities.invokeLater(() -> {
                if (s != null) {
                    lblStatTotalUsers.setText(String.valueOf(s.totalUsers));
                    lblStatTotalKeys.setText(String.valueOf(s.totalLicenses));
                }
                if (o != null) {
                    lblStatOnlineUsers.setText(String.valueOf(o.size()));
                    onlineUsersListModel.clear();
                    for (String u : o) {
                        onlineUsersListModel.addElement(u);
                    }
                }
                updateStatus("Telemetry metrics updated.", COLOR_SUCCESS);
            });
        }).start();
    }

    private static class SessionListCellRenderer extends JPanel implements ListCellRenderer<String> {
        private final JLabel lblDot = new JLabel("●");
        private final JLabel lblName = new JLabel();
        private final PillBadge badgeOnline = new PillBadge("ONLINE", new Color(52, 211, 153), new Color(17, 37, 30), new Color(24, 92, 70), 4);

        public SessionListCellRenderer() {
            setLayout(new BorderLayout(10, 0));
            setOpaque(true);
            setBackground(new Color(14, 16, 23));
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(31, 36, 51), 1),
                new EmptyBorder(8, 12, 8, 12)
            ));

            lblDot.setForeground(COLOR_SUCCESS);
            lblDot.setFont(new Font("Segoe UI", Font.BOLD, 12));

            lblName.setForeground(Color.WHITE);
            lblName.setFont(new Font("Segoe UI", Font.BOLD, 12));

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            left.setOpaque(false);
            left.add(lblDot);
            left.add(lblName);

            add(left, BorderLayout.WEST);
            add(badgeOnline, BorderLayout.EAST);
        }

        @Override public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
            lblName.setText(value != null ? value : "User");
            return this;
        }
    }

    private JPanel buildSecurityTab() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel("Account & Security");
        lblTitle.setFont(FONT_DASH_TITLE);
        lblTitle.setForeground(COLOR_TEXT_HEAD);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSub = new JLabel("Manage credentials, upgrades, and two-factor authentication.");
        lblSub.setFont(FONT_BODY);
        lblSub.setForeground(COLOR_TEXT_MUTED);
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(lblTitle);
        p.add(Box.createVerticalStrut(4));
        p.add(lblSub);
        p.add(Box.createVerticalStrut(20));

        ModernCard cardUsername = new ModernCard(12, COLOR_CARD_BG, COLOR_BORDER_SUBTLE);
        cardUsername.setLayout(new BoxLayout(cardUsername, BoxLayout.Y_AXIS));
        cardUsername.setBorder(new EmptyBorder(20, 22, 20, 22));
        cardUsername.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardUsername.setMaximumSize(new Dimension(Integer.MAX_VALUE, 185));

        cardUsername.add(createSectionHeader("Change Username"));
        cardUsername.add(Box.createVerticalStrut(4));

        JLabel lblUserWarn = new JLabel("<html><body style='margin:0; padding:0; text-align:left; width:670px; color:#6B6B74; font-size:11.5px;'>This immediately terminates active sessions — you will be prompted to sign in again with the new username.</body></html>");
        lblUserWarn.setFont(FONT_BODY_SM);
        lblUserWarn.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardUsername.add(lblUserWarn);
        cardUsername.add(Box.createVerticalStrut(12));

        JPanel rowChUser = new JPanel(new GridLayout(1, 2, 14, 0));
        rowChUser.setOpaque(false);
        rowChUser.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowChUser.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        ModernTextField txtNewUser = new ModernTextField(FieldIconType.NONE, "Enter new username");
        ModernPasswordField txtConfirmPass = new ModernPasswordField("Confirm password");

        JPanel c1 = new JPanel(new BorderLayout(0, 4));
        c1.setOpaque(false);
        JLabel l1 = new JLabel("NEW USERNAME"); l1.setFont(FONT_LABEL); l1.setForeground(COLOR_TEXT_SEC);
        c1.add(l1, BorderLayout.NORTH); c1.add(txtNewUser, BorderLayout.CENTER);

        JPanel c2 = new JPanel(new BorderLayout(0, 4));
        c2.setOpaque(false);
        JLabel l2 = new JLabel("CONFIRM PASSWORD"); l2.setFont(FONT_LABEL); l2.setForeground(COLOR_TEXT_SEC);
        c2.add(l2, BorderLayout.NORTH); c2.add(txtConfirmPass, BorderLayout.CENTER);

        rowChUser.add(c1);
        rowChUser.add(c2);
        cardUsername.add(rowChUser);
        cardUsername.add(Box.createVerticalStrut(14));

        ModernButton btnChangeUser = ModernButton.primaryButton("Update Username", e -> {
            String nu = txtNewUser.getText().trim();
            String cp = txtConfirmPass.getPasswordString().trim();
            if (nu.isEmpty() || cp.isEmpty()) return;
            new Thread(() -> {
                boolean ok = api.changeUsername(nu, cp);
                SwingUtilities.invokeLater(() -> {
                    updateStatus(ok ? "Username updated successfully. Please re-login." : api.response.message, ok ? COLOR_SUCCESS : COLOR_ERROR);
                    if (ok) performLogout();
                });
            }).start();
        });
        btnChangeUser.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnChangeUser.setPreferredSize(new Dimension(150, 36));
        cardUsername.add(btnChangeUser);

        p.add(cardUsername);
        p.add(Box.createVerticalStrut(18));

        ModernCard cardUpgrade = new ModernCard(12, COLOR_CARD_BG, COLOR_BORDER_SUBTLE);
        cardUpgrade.setLayout(new BoxLayout(cardUpgrade, BoxLayout.Y_AXIS));
        cardUpgrade.setBorder(new EmptyBorder(20, 22, 20, 22));
        cardUpgrade.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardUpgrade.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        cardUpgrade.add(createSectionHeader("Upgrade Plan with License Key"));
        cardUpgrade.add(Box.createVerticalStrut(10));

        JLabel lKey = new JLabel("LICENSE KEY");
        lKey.setFont(FONT_LABEL);
        lKey.setForeground(COLOR_TEXT_SEC);
        lKey.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardUpgrade.add(lKey);
        cardUpgrade.add(Box.createVerticalStrut(4));

        ModernTextField txtUpgradeKey = new ModernTextField(FieldIconType.NONE, "Enter license key to extend subscription");
        txtUpgradeKey.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtUpgradeKey.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        cardUpgrade.add(txtUpgradeKey);
        cardUpgrade.add(Box.createVerticalStrut(12));

        ModernButton btnApplyUpgrade = ModernButton.primaryButton("Apply Upgrade", e -> {
            String k = txtUpgradeKey.getText().trim();
            if (k.isEmpty() || api.user == null) return;
            new Thread(() -> {
                boolean ok = api.upgrade(api.user.username, k);
                SwingUtilities.invokeLater(() -> {
                    updateStatus(ok ? "Upgrade applied successfully!" : api.response.message, ok ? COLOR_SUCCESS : COLOR_ERROR);
                    if (ok) refreshOverview();
                });
            }).start();
        });
        btnApplyUpgrade.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnApplyUpgrade.setPreferredSize(new Dimension(130, 36));
        cardUpgrade.add(btnApplyUpgrade);

        p.add(cardUpgrade);
        p.add(Box.createVerticalStrut(18));

        ModernCard card2fa = new ModernCard(12, COLOR_CARD_BG, COLOR_BORDER_SUBTLE);
        card2fa.setLayout(new BoxLayout(card2fa, BoxLayout.Y_AXIS));
        card2fa.setBorder(new EmptyBorder(20, 22, 20, 22));
        card2fa.setAlignmentX(Component.LEFT_ALIGNMENT);
        card2fa.setMaximumSize(new Dimension(Integer.MAX_VALUE, 460));

        JPanel head2fa = new JPanel(new BorderLayout());
        head2fa.setOpaque(false);
        head2fa.setAlignmentX(Component.LEFT_ALIGNMENT);
        head2fa.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        head2fa.add(createSectionHeader("Two-Factor Authentication"), BorderLayout.WEST);

        badge2faStatus = new PillBadge("SET UP AVAILABLE", COLOR_TEXT_MUTED, new Color(113, 113, 122, 34), new Color(113, 113, 122, 68), 5);
        head2fa.add(badge2faStatus, BorderLayout.EAST);
        card2fa.add(head2fa);
        card2fa.add(Box.createVerticalStrut(14));

        panel2faStart = new JPanel();
        panel2faStart.setOpaque(false);
        panel2faStart.setLayout(new BoxLayout(panel2faStart, BoxLayout.Y_AXIS));
        panel2faStart.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl2faIntro = new JLabel("<html><body style='margin:0; padding:0; text-align:left; color:#A1A1AA; font-size:12px;'>Protect this account with an authenticator app (Google Authenticator, Authy, 1Password, etc.).</body></html>");
        lbl2faIntro.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl2faIntro.setHorizontalAlignment(SwingConstants.LEFT);
        panel2faStart.add(lbl2faIntro);
        panel2faStart.add(Box.createVerticalStrut(14));

        ModernButton btnSetup2fa = ModernButton.primaryButton("Set Up 2FA", e -> start2faSetup());
        btnSetup2fa.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnSetup2fa.setPreferredSize(new Dimension(120, 36));
        panel2faStart.add(btnSetup2fa);
        card2fa.add(panel2faStart);

        panel2faQr = new JPanel();
        panel2faQr.setOpaque(false);
        panel2faQr.setLayout(new BoxLayout(panel2faQr, BoxLayout.Y_AXIS));
        panel2faQr.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel2faQr.setVisible(false);

        JLabel lblScan = new JLabel("Scan this QR with your authenticator app:");
        lblScan.setFont(FONT_BODY);
        lblScan.setForeground(Color.WHITE);
        lblScan.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel2faQr.add(lblScan);
        panel2faQr.add(Box.createVerticalStrut(10));

        lbl2faQrImage = new JLabel();
        lbl2faQrImage.setOpaque(true);
        lbl2faQrImage.setBackground(Color.WHITE);
        lbl2faQrImage.setPreferredSize(new Dimension(180, 180));
        lbl2faQrImage.setMaximumSize(new Dimension(180, 180));
        lbl2faQrImage.setHorizontalAlignment(SwingConstants.CENTER);
        lbl2faQrImage.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel2faQr.add(lbl2faQrImage);
        panel2faQr.add(Box.createVerticalStrut(12));

        JLabel lblSecretHeader = new JLabel("Or enter this key manually:");
        lblSecretHeader.setFont(FONT_LABEL);
        lblSecretHeader.setForeground(COLOR_TEXT_SEC);
        lblSecretHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel2faQr.add(lblSecretHeader);
        panel2faQr.add(Box.createVerticalStrut(4));

        JPanel secretRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        secretRow.setOpaque(false);
        secretRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        txt2faSecretLabel = new JLabel("XXXX-XXXX-XXXX");
        txt2faSecretLabel.setFont(new Font("Consolas", Font.BOLD, 13));
        txt2faSecretLabel.setForeground(COLOR_BLUE_GLOW);

        ModernButton btnCopySecret = ModernButton.ghostButton("Copy Key", e -> {
            String s = txt2faSecretLabel.getText();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(s), null);
            updateStatus("2FA secret copied to clipboard.", COLOR_SUCCESS);
        });

        secretRow.add(txt2faSecretLabel);
        secretRow.add(btnCopySecret);
        panel2faQr.add(secretRow);
        panel2faQr.add(Box.createVerticalStrut(12));

        JLabel lblConfirmH = new JLabel("CONFIRMATION CODE");
        lblConfirmH.setFont(FONT_LABEL);
        lblConfirmH.setForeground(COLOR_TEXT_SEC);
        lblConfirmH.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel2faQr.add(lblConfirmH);
        panel2faQr.add(Box.createVerticalStrut(4));

        txt2faConfirmCode = new ModernTextField(FieldIconType.NONE, "000000");
        txt2faConfirmCode.setAlignmentX(Component.LEFT_ALIGNMENT);
        txt2faConfirmCode.setPreferredSize(new Dimension(160, 38));
        txt2faConfirmCode.setMaximumSize(new Dimension(160, 38));
        panel2faQr.add(txt2faConfirmCode);
        panel2faQr.add(Box.createVerticalStrut(12));

        JPanel qrBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        qrBtns.setOpaque(false);
        qrBtns.setAlignmentX(Component.LEFT_ALIGNMENT);

        ModernButton btnConfirm2fa = ModernButton.primaryButton("Confirm & Enable 2FA", e -> confirm2fa());
        ModernButton btnCancel2fa = ModernButton.ghostButton("Cancel", e -> {
            panel2faQr.setVisible(false);
            panel2faStart.setVisible(true);
        });

        qrBtns.add(btnConfirm2fa);
        qrBtns.add(btnCancel2fa);
        panel2faQr.add(qrBtns);
        card2fa.add(panel2faQr);

        panel2faBackup = new JPanel();
        panel2faBackup.setOpaque(false);
        panel2faBackup.setLayout(new BoxLayout(panel2faBackup, BoxLayout.Y_AXIS));
        panel2faBackup.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel2faBackup.setVisible(false);

        JLabel lblBackupTitle = new JLabel("✓ 2FA enabled successfully. Save these one-time backup codes somewhere safe:");
        lblBackupTitle.setFont(FONT_BODY);
        lblBackupTitle.setForeground(COLOR_SUCCESS);
        lblBackupTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel2faBackup.add(lblBackupTitle);
        panel2faBackup.add(Box.createVerticalStrut(10));

        backupCodesContainer = new JPanel();
        backupCodesContainer.setOpaque(false);
        backupCodesContainer.setLayout(new BoxLayout(backupCodesContainer, BoxLayout.Y_AXIS));
        backupCodesContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel2faBackup.add(backupCodesContainer);
        panel2faBackup.add(Box.createVerticalStrut(12));

        ModernButton btnFinish2fa = ModernButton.ghostButton("Done", e -> {
            panel2faBackup.setVisible(false);
            panel2faDisable.setVisible(true);
            badge2faStatus.setText("2FA ACTIVE");
            badge2faStatus.setTextAndPillColors(COLOR_SUCCESS, new Color(16, 185, 129, 32), new Color(16, 185, 129, 64));
        });
        btnFinish2fa.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel2faBackup.add(btnFinish2fa);
        card2fa.add(panel2faBackup);

        panel2faDisable = new JPanel();
        panel2faDisable.setOpaque(false);
        panel2faDisable.setLayout(new BoxLayout(panel2faDisable, BoxLayout.Y_AXIS));
        panel2faDisable.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel2faDisable.setVisible(false);

        JLabel lblDisIntro = new JLabel("<html><body style='margin:0; padding:0; text-align:left; color:#A1A1AA; font-size:11.5px;'>Disabling requires your account password AND a current 6-digit code from your authenticator app.</body></html>");
        lblDisIntro.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblDisIntro.setHorizontalAlignment(SwingConstants.LEFT);
        panel2faDisable.add(lblDisIntro);
        panel2faDisable.add(Box.createVerticalStrut(12));

        JPanel disRow = new JPanel(new GridLayout(1, 2, 14, 0));
        disRow.setOpaque(false);
        disRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        disRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        txt2faDisablePassword = new ModernPasswordField("Password");
        txt2faDisableCode = new ModernTextField(FieldIconType.NONE, "000000");

        JPanel dp1 = new JPanel(new BorderLayout(0, 4)); dp1.setOpaque(false);
        JLabel dpl1 = new JLabel("PASSWORD"); dpl1.setFont(FONT_LABEL); dpl1.setForeground(COLOR_TEXT_SEC);
        dp1.add(dpl1, BorderLayout.NORTH); dp1.add(txt2faDisablePassword, BorderLayout.CENTER);

        JPanel dp2 = new JPanel(new BorderLayout(0, 4)); dp2.setOpaque(false);
        JLabel dpl2 = new JLabel("2FA CODE"); dpl2.setFont(FONT_LABEL); dpl2.setForeground(COLOR_TEXT_SEC);
        dp2.add(dpl2, BorderLayout.NORTH); dp2.add(txt2faDisableCode, BorderLayout.CENTER);

        disRow.add(dp1);
        disRow.add(dp2);
        panel2faDisable.add(disRow);
        panel2faDisable.add(Box.createVerticalStrut(14));

        ModernButton btnDisable2fa = ModernButton.dangerButton("Disable 2FA", e -> disable2fa());
        btnDisable2fa.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnDisable2fa.setPreferredSize(new Dimension(130, 36));
        panel2faDisable.add(btnDisable2fa);
        card2fa.add(panel2faDisable);

        p.add(card2fa);
        p.add(Box.createVerticalStrut(18));

        ModernCard cardLog = new ModernCard(12, COLOR_CARD_BG, COLOR_BORDER_SUBTLE);
        cardLog.setLayout(new BoxLayout(cardLog, BoxLayout.Y_AXIS));
        cardLog.setBorder(new EmptyBorder(20, 22, 20, 22));
        cardLog.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardLog.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        cardLog.add(createSectionHeader("Send Audit Log to Server"));
        cardLog.add(Box.createVerticalStrut(10));

        JLabel lMsg = new JLabel("MESSAGE");
        lMsg.setFont(FONT_LABEL);
        lMsg.setForeground(COLOR_TEXT_SEC);
        lMsg.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardLog.add(lMsg);
        cardLog.add(Box.createVerticalStrut(4));

        ModernTextField txtAuditMsg = new ModernTextField(FieldIconType.NONE, "Enter log message to store in dashboard audit trail...");
        txtAuditMsg.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtAuditMsg.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        cardLog.add(txtAuditMsg);
        cardLog.add(Box.createVerticalStrut(12));

        ModernButton btnSendLog = ModernButton.ghostButton("Send Audit Log", e -> {
            String m = txtAuditMsg.getText().trim();
            if (m.isEmpty()) return;
            new Thread(() -> {
                boolean ok = api.log(m);
                SwingUtilities.invokeLater(() -> {
                    updateStatus(ok ? "Audit log transmitted to server." : "Failed to record log.", ok ? COLOR_SUCCESS : COLOR_ERROR);
                    if (ok) txtAuditMsg.setText("");
                });
            }).start();
        });
        btnSendLog.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnSendLog.setPreferredSize(new Dimension(140, 36));
        cardLog.add(btnSendLog);

        p.add(cardLog);
        p.add(Box.createVerticalStrut(18));

        ModernCard cardProt = new ModernCard(12, COLOR_CARD_BG, COLOR_BORDER_SUBTLE);
        cardProt.setLayout(new BoxLayout(cardProt, BoxLayout.Y_AXIS));
        cardProt.setBorder(new EmptyBorder(20, 22, 20, 22));
        cardProt.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardProt.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        cardProt.add(createSectionHeader("Hardware & Integrity Protections"));
        cardProt.add(Box.createVerticalStrut(14));

        JLabel lHwidBl = new JLabel("HWID BLACKLIST CHECKER");
        lHwidBl.setFont(FONT_LABEL);
        lHwidBl.setForeground(COLOR_TEXT_SEC);
        lHwidBl.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardProt.add(lHwidBl);
        cardProt.add(Box.createVerticalStrut(4));

        JPanel rowHwidBl = new JPanel(new BorderLayout(8, 0));
        rowHwidBl.setOpaque(false);
        rowHwidBl.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowHwidBl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        ModernTextField txtBlHwid = new ModernTextField(FieldIconType.NONE, "Enter HWID or leave empty for current machine");
        ModernButton btnCheckBl = ModernButton.ghostButton("Check HWID", e -> {
            String h = txtBlHwid.getText().trim();
            new Thread(() -> {
                boolean blacklisted = api.checkBlacklist(h.isEmpty() ? null : h);
                SwingUtilities.invokeLater(() -> {
                    updateStatus(blacklisted ? "HWID is BLACKLISTED on server!" : "HWID is clean (not blacklisted).", blacklisted ? COLOR_ERROR : COLOR_SUCCESS);
                });
            }).start();
        });
        btnCheckBl.setPreferredSize(new Dimension(130, 36));
        rowHwidBl.add(txtBlHwid, BorderLayout.CENTER);
        rowHwidBl.add(btnCheckBl, BorderLayout.EAST);
        cardProt.add(rowHwidBl);
        cardProt.add(Box.createVerticalStrut(14));

        JLabel lHash = new JLabel("FILE INTEGRITY AUTHENTICATOR (SHA-256)");
        lHash.setFont(FONT_LABEL);
        lHash.setForeground(COLOR_TEXT_SEC);
        lHash.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardProt.add(lHash);
        cardProt.add(Box.createVerticalStrut(4));

        JPanel rowHash = new JPanel(new BorderLayout(8, 0));
        rowHash.setOpaque(false);
        rowHash.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowHash.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        ModernTextField txtHash = new ModernTextField(FieldIconType.NONE, "Enter file SHA-256 hash to verify against server signatures...");
        ModernButton btnVerifyHash = ModernButton.ghostButton("Verify Hash", e -> {
            String hsh = txtHash.getText().trim();
            if (hsh.isEmpty()) return;
            new Thread(() -> {
                boolean ok = api.verifyFileHash(hsh);
                SwingUtilities.invokeLater(() -> {
                    updateStatus(ok ? "File SHA-256 signature MATCHES server release." : "Hash mismatch or unrecognized file.", ok ? COLOR_SUCCESS : COLOR_ERROR);
                });
            }).start();
        });
        btnVerifyHash.setPreferredSize(new Dimension(130, 36));
        rowHash.add(txtHash, BorderLayout.CENTER);
        rowHash.add(btnVerifyHash, BorderLayout.EAST);
        cardProt.add(rowHash);
        cardProt.add(Box.createVerticalStrut(14));

        JLabel lBan = new JLabel("EMERGENCY KILLSWITCH / SELF-BAN");
        lBan.setFont(FONT_LABEL);
        lBan.setForeground(COLOR_ERROR);
        lBan.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardProt.add(lBan);
        cardProt.add(Box.createVerticalStrut(4));

        JPanel rowBan = new JPanel(new BorderLayout(8, 0));
        rowBan.setOpaque(false);
        rowBan.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowBan.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        ModernTextField txtBanReason = new ModernTextField(FieldIconType.NONE, "Reason for self-ban / killswitch...");
        ModernButton btnBan = ModernButton.dangerButton("Trigger Ban", e -> {
            String r = txtBanReason.getText().trim();
            new Thread(() -> {
                boolean ok = api.ban(r.isEmpty() ? "User initiated emergency self-ban" : r);
                SwingUtilities.invokeLater(() -> {
                    updateStatus(ok ? "Emergency ban applied." : api.response.message, COLOR_ERROR);
                    if (ok) performLogout();
                });
            }).start();
        });
        btnBan.setPreferredSize(new Dimension(130, 36));
        rowBan.add(txtBanReason, BorderLayout.CENTER);
        rowBan.add(btnBan, BorderLayout.EAST);
        cardProt.add(rowBan);

        p.add(cardProt);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private void start2faSetup() {
        new Thread(() -> {
            NightAuthApi.TwoFactorSetupData s = api.setup2FA();
            SwingUtilities.invokeLater(() -> {
                if (s != null && s.secret != null) {
                    txt2faSecretLabel.setText(s.secret);
                    if (s.qrDataUrl != null && !s.qrDataUrl.isEmpty()) {
                        try {
                            String b64 = s.qrDataUrl;
                            if (b64.contains(",")) b64 = b64.substring(b64.indexOf(",") + 1);
                            byte[] bytes = Base64.getDecoder().decode(b64);
                            BufferedImage bi = ImageIO.read(new ByteArrayInputStream(bytes));
                            if (bi != null) {
                                lbl2faQrImage.setIcon(new ImageIcon(bi.getScaledInstance(180, 180, Image.SCALE_SMOOTH)));
                            }
                        } catch (Exception ignored) {}
                    }
                    panel2faStart.setVisible(false);
                    panel2faQr.setVisible(true);
                    updateStatus("2FA setup initiated. Scan QR or enter secret.", COLOR_BLUE_GLOW);
                } else {
                    updateStatus("Failed to initiate 2FA setup: " + api.response.message, COLOR_ERROR);
                }
            });
        }).start();
    }

    private void confirm2fa() {
        String c = txt2faConfirmCode.getText().trim();
        if (c.isEmpty()) return;
        new Thread(() -> {
            NightAuthApi.TwoFactorConfirmData res = api.confirm2FA(c);
            SwingUtilities.invokeLater(() -> {
                if (res != null) {
                    panel2faQr.setVisible(false);
                    backupCodesContainer.removeAll();
                    if (res.backupCodes != null) {
                        for (String code : res.backupCodes) {
                            JLabel l = new JLabel(code);
                            l.setFont(FONT_MONO);
                            l.setForeground(Color.WHITE);
                            backupCodesContainer.add(l);
                        }
                    }
                    panel2faBackup.setVisible(true);
                    updateStatus("2FA verified and activated successfully!", COLOR_SUCCESS);
                } else {
                    updateStatus("Invalid 2FA code: " + api.response.message, COLOR_ERROR);
                }
            });
        }).start();
    }

    private void disable2fa() {
        String pwd = txt2faDisablePassword.getPasswordString().trim();
        String c = txt2faDisableCode.getText().trim();
        if (pwd.isEmpty() || c.isEmpty()) return;
        new Thread(() -> {
            boolean ok = api.disable2FA(pwd, c);
            SwingUtilities.invokeLater(() -> {
                if (ok) {
                    panel2faDisable.setVisible(false);
                    panel2faStart.setVisible(true);
                    badge2faStatus.setText("SET UP AVAILABLE");
                    badge2faStatus.setTextAndPillColors(COLOR_TEXT_MUTED, new Color(113, 113, 122, 34), new Color(113, 113, 122, 68));
                    updateStatus("Two-factor authentication disabled.", COLOR_SUCCESS);
                } else {
                    updateStatus("Failed to disable 2FA: " + api.response.message, COLOR_ERROR);
                }
            });
        }).start();
    }

    private JPanel buildFilesHooksTab() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel("Files & Webhooks");
        lblTitle.setFont(FONT_DASH_TITLE);
        lblTitle.setForeground(COLOR_TEXT_HEAD);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSub = new JLabel("Secure server asset distribution and cloud webhook dispatch.");
        lblSub.setFont(FONT_BODY);
        lblSub.setForeground(COLOR_TEXT_MUTED);
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(lblTitle);
        p.add(Box.createVerticalStrut(4));
        p.add(lblSub);
        p.add(Box.createVerticalStrut(20));

        ModernCard cardFile = new ModernCard(12, COLOR_CARD_BG, COLOR_BORDER_SUBTLE);
        cardFile.setLayout(new BoxLayout(cardFile, BoxLayout.Y_AXIS));
        cardFile.setBorder(new EmptyBorder(20, 22, 20, 22));
        cardFile.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardFile.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));

        cardFile.add(createSectionHeader("Protected Remote File Downloader"));
        cardFile.add(Box.createVerticalStrut(12));

        JPanel rowFile = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        rowFile.setOpaque(false);
        rowFile.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowFile.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        ModernTextField txtFileId = new ModernTextField(FieldIconType.NONE, "File ID (e.g. 1)");
        txtFileId.setPreferredSize(new Dimension(280, 38));

        JLabel lblFileRes = new JLabel("Protected binary payload downloads directly into memory.");
        lblFileRes.setFont(FONT_BODY_SM);
        lblFileRes.setForeground(COLOR_TEXT_MUTED);
        lblFileRes.setAlignmentX(Component.LEFT_ALIGNMENT);

        ModernButton btnDownload = ModernButton.primaryButton("Download File", e -> {
            String fid = txtFileId.getText().trim();
            if (fid.isEmpty()) return;
            new Thread(() -> {
                byte[] bytes = api.downloadFile(fid);
                SwingUtilities.invokeLater(() -> {
                    if (bytes != null) {
                        lblFileRes.setText("✓ File " + fid + " downloaded successfully (" + bytes.length + " bytes in memory).");
                        lblFileRes.setForeground(COLOR_SUCCESS);
                        updateStatus("File " + fid + " downloaded (" + bytes.length + " bytes).", COLOR_SUCCESS);
                    } else {
                        lblFileRes.setText("Download failed: " + api.response.message);
                        lblFileRes.setForeground(COLOR_ERROR);
                        updateStatus("Download failed.", COLOR_ERROR);
                    }
                });
            }).start();
        });
        btnDownload.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnDownload.setPreferredSize(new Dimension(140, 38));

        rowFile.add(txtFileId);
        rowFile.add(btnDownload);

        cardFile.add(rowFile);
        cardFile.add(Box.createVerticalStrut(8));
        cardFile.add(lblFileRes);

        p.add(cardFile);
        p.add(Box.createVerticalStrut(20));

        ModernCard cardHook = new ModernCard(12, COLOR_CARD_BG, COLOR_BORDER_SUBTLE);
        cardHook.setLayout(new BoxLayout(cardHook, BoxLayout.Y_AXIS));
        cardHook.setBorder(new EmptyBorder(20, 22, 20, 22));
        cardHook.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardHook.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        cardHook.add(createSectionHeaderWithSub("Webhook Trigger", "•  Secure cloud webhook dispatch", COLOR_ACCENT_PURPLE));
        cardHook.add(Box.createVerticalStrut(12));

        JPanel rowHook = new JPanel(new GridBagLayout());
        rowHook.setOpaque(false);
        rowHook.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowHook.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 0, 10);

        ModernTextField txtHookId = new ModernTextField(FieldIconType.NONE, "Webhook ID");
        txtHookId.setPreferredSize(new Dimension(220, 38));
        g.gridx = 0; g.weightx = 0.3;
        rowHook.add(txtHookId, g);

        ModernTextField txtHookPayload = new ModernTextField(FieldIconType.NONE, "Optional JSON payload (e.g. {\"action\":\"ping\"})");
        txtHookPayload.setPreferredSize(new Dimension(300, 38));
        g.gridx = 1; g.weightx = 0.5;
        rowHook.add(txtHookPayload, g);

        JLabel lblHookResult = new JLabel("Server response will appear here...");
        lblHookResult.setFont(FONT_BODY_SM);
        lblHookResult.setForeground(COLOR_TEXT_MUTED);
        lblHookResult.setAlignmentX(Component.LEFT_ALIGNMENT);

        ModernButton btnTriggerHook = ModernButton.primaryButton("Trigger Webhook", e -> {
            String hid = txtHookId.getText().trim();
            String pld = txtHookPayload.getText().trim();
            if (hid.isEmpty()) return;
            new Thread(() -> {
                Map<String, Object> map = new LinkedHashMap<>();
                if (!pld.isEmpty()) map.put("data", pld);
                NightAuthApi.WebhookResult r = api.triggerWebhook(hid, map);
                SwingUtilities.invokeLater(() -> {
                    if (r != null) {
                        lblHookResult.setText("Status: " + r.status + " | Response: " + r.data);
                        lblHookResult.setForeground(COLOR_SUCCESS);
                        updateStatus("Webhook executed successfully.", COLOR_SUCCESS);
                    } else {
                        lblHookResult.setText("Webhook failed: " + api.response.message);
                        lblHookResult.setForeground(COLOR_ERROR);
                        updateStatus("Webhook trigger refused.", COLOR_ERROR);
                    }
                });
            }).start();
        });
        btnTriggerHook.setPreferredSize(new Dimension(140, 38));
        g.gridx = 2; g.weightx = 0.2; g.insets = new Insets(0, 0, 0, 0);
        rowHook.add(btnTriggerHook, g);

        cardHook.add(rowHook);
        cardHook.add(Box.createVerticalStrut(8));
        cardHook.add(lblHookResult);

        p.add(cardHook);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private static JPanel createSectionHeader(String title) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        p.add(new AccentBar(3, 13, COLOR_ACCENT_BLUE));
        JLabel l = new JLabel(title);
        l.setFont(FONT_SECTION);
        l.setForeground(COLOR_TEXT_HEAD);
        p.add(l);
        return p;
    }

    private static JPanel createSectionHeaderWithSub(String title, String subtitle, Color barColor) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        p.add(new AccentBar(3, 13, barColor));
        JLabel l = new JLabel(title);
        l.setFont(FONT_SECTION);
        l.setForeground(COLOR_TEXT_HEAD);
        p.add(l);

        JLabel sub = new JLabel(subtitle);
        sub.setFont(FONT_BODY_SM);
        sub.setForeground(COLOR_TEXT_MUTED);
        p.add(sub);
        return p;
    }

    private static JScrollPane createModernScrollPane(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        sp.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        sp.getHorizontalScrollBar().setUI(new ModernScrollBarUI());
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(7, 0));
        sp.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 0));
        return sp;
    }

    public static class ScrollablePanel extends JPanel implements Scrollable {
        public ScrollablePanel(LayoutManager layout) {
            super(layout);
        }

        public ScrollablePanel() {
            super();
        }

        @Override
        public Dimension getPreferredSize() {
            for (Component c : getComponents()) {
                if (c.isVisible()) {
                    Insets ins = getInsets();
                    Dimension d = c.getPreferredSize();
                    int w = d.width + ins.left + ins.right;
                    int h = d.height + ins.top + ins.bottom;
                    return new Dimension(w, h);
                }
            }
            return super.getPreferredSize();
        }

        @Override
        public Dimension getMinimumSize() {
            for (Component c : getComponents()) {
                if (c.isVisible()) {
                    Insets ins = getInsets();
                    Dimension d = c.getMinimumSize();
                    return new Dimension(d.width + ins.left + ins.right, d.height + ins.top + ins.bottom);
                }
            }
            return super.getMinimumSize();
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 64;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private static JTable createModernTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setOpaque(false);
        table.setBackground(new Color(10, 10, 12));
        table.setForeground(COLOR_TEXT_HEAD);
        table.setGridColor(new Color(31, 36, 51));
        table.setRowHeight(34);
        table.setFont(FONT_BODY);
        table.setSelectionBackground(new Color(30, 41, 59));
        table.setSelectionForeground(Color.WHITE);
        table.setShowVerticalLines(false);

        JTableHeader header = table.getTableHeader();
        header.setBackground(COLOR_CARD_HEADER);
        header.setForeground(COLOR_TEXT_SEC);
        header.setFont(FONT_LABEL);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER_SUBTLE));
        header.setPreferredSize(new Dimension(0, 32));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        centerRenderer.setOpaque(false);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        return table;
    }

    public static class SubscriptionTableCellRenderer extends DefaultTableCellRenderer {
        private final JPanel panelSubBadge = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 3));
        private final PillBadge badgeSub = new PillBadge("", new Color(147, 197, 253), new Color(26, 34, 52), new Color(38, 56, 92), 5);

        private final JPanel panelTimeBadge = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 3));
        private final PillBadge badgeTime = new PillBadge("", new Color(52, 211, 153), new Color(15, 36, 27), new Color(26, 74, 53), 4);

        public SubscriptionTableCellRenderer() {
            panelSubBadge.setOpaque(false);
            panelSubBadge.add(badgeSub);

            panelTimeBadge.setOpaque(false);
            panelTimeBadge.add(badgeTime);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String val = (value != null) ? value.toString() : "";

            if (column == 0) {
                badgeSub.setText(val);
                panelSubBadge.setOpaque(isSelected);
                if (isSelected) panelSubBadge.setBackground(new Color(30, 41, 59));
                return panelSubBadge;
            } else if (column == 3) {
                badgeTime.setText(val);
                panelTimeBadge.setOpaque(isSelected);
                if (isSelected) panelTimeBadge.setBackground(new Color(30, 41, 59));
                return panelTimeBadge;
            }

            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setOpaque(isSelected);
            if (isSelected) {
                label.setBackground(new Color(30, 41, 59));
            }
            if (column == 1) {
                label.setFont(FONT_MONO);
                label.setForeground(COLOR_TEXT_HEAD);
            } else if (column == 2) {
                label.setFont(FONT_BODY);
                label.setForeground(COLOR_TEXT_SEC);
            }
            label.setBorder(new EmptyBorder(0, 10, 0, 10));
            return label;
        }
    }

    public static class VariablesTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setOpaque(isSelected);
            if (isSelected) {
                label.setBackground(new Color(30, 41, 59));
            }
            if (column == 0) {
                label.setFont(FONT_MONO);
                label.setForeground(COLOR_BLUE_GLOW);
            } else {
                label.setFont(FONT_BODY);
                label.setForeground(COLOR_TEXT_HEAD);
            }
            label.setBorder(new EmptyBorder(0, 10, 0, 10));
            return label;
        }
    }

    public static class AccentBar extends JComponent {
        private final int barWidth;
        private final int barHeight;
        private final Color color;

        public AccentBar(int w, int h, Color col) {
            this.barWidth = w;
            this.barHeight = h;
            this.color = col;
            setPreferredSize(new Dimension(w, h));
            setMinimumSize(new Dimension(w, h));
            setMaximumSize(new Dimension(w, h));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(0, (getHeight() - barHeight) / 2, barWidth, barHeight, 2, 2);
            g2.dispose();
        }
    }

    public static class CheckmarkBadge extends JComponent {
        public CheckmarkBadge() {
            setPreferredSize(new Dimension(22, 22));
            setMinimumSize(new Dimension(22, 22));
            setMaximumSize(new Dimension(22, 22));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            g2.setColor(COLOR_SUCCESS_BG);
            g2.fillRoundRect(0, 0, 21, 21, 5, 5);
            g2.setColor(COLOR_SUCCESS_BORDER);
            g2.drawRoundRect(0, 0, 21, 21, 5, 5);

            g2.setColor(COLOR_SUCCESS);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(6, 11, 9, 14);
            g2.drawLine(9, 14, 15, 7);
            g2.dispose();
        }
    }

    public static class ModernCard extends JPanel {
        private final int radius;
        private final Color bgColor;
        private final Color borderColor;

        public ModernCard(int radius, Color bg, Color border) {
            this.radius = radius;
            this.bgColor = bg;
            this.borderColor = border;
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static class PillBadge extends JComponent {
        private String text;
        private Color textColor;
        private Color bgColor;
        private Color borderColor;
        private final int radius;

        public PillBadge(String text, Color textCol, Color bgCol, Color borderCol, int rad) {
            this.text = text;
            this.textColor = textCol;
            this.bgColor = bgCol;
            this.borderColor = borderCol;
            this.radius = rad;
            setFont(new Font("Segoe UI", Font.BOLD, 10));
            setBorder(new EmptyBorder(4, 10, 4, 10));
        }

        public void setText(String text) {
            this.text = text;
            revalidate();
            repaint();
        }

        public void setTextAndPillColors(Color textCol, Color bgCol, Color borderCol) {
            this.textColor = textCol;
            this.bgColor = bgCol;
            this.borderColor = borderCol;
            revalidate();
            repaint();
        }

        @Override public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int w = fm.stringWidth(text) + 20;
            int h = fm.getHeight() + 8;
            return new Dimension(w, h);
        }

        @Override public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius * 2, radius * 2);
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius * 2, radius * 2);

            g2.setColor(textColor);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int textW = fm.stringWidth(text);
            int tx = Math.max(8, (getWidth() - textW) / 2);
            int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, tx, ty);
            g2.dispose();
        }
    }

    public enum FieldIconType {
        NONE,
        USER,
        LOCK,
        SHIELD,
        EMAIL,
        KEY,
        TOKEN,
        CHAT,
        DOWNLOAD,
        WEBHOOK
    }

    public static class ModernTextField extends JTextField {
        private final FieldIconType iconType;
        private final String placeholder;
        private boolean isHovered = false;

        public ModernTextField(FieldIconType icon, String placeholder) {
            this.iconType = icon != null ? icon : FieldIconType.NONE;
            this.placeholder = placeholder;
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setFont(FONT_INPUT);
            setForeground(COLOR_TEXT_HEAD);
            setCaretColor(COLOR_ACCENT_BLUE);
            setBorder(new EmptyBorder(0, (iconType != FieldIconType.NONE) ? 34 : 12, 0, 12));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
            });
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { repaint(); }
                @Override public void focusLost(FocusEvent e) { repaint(); }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(super.getPreferredSize().width, 38);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, 38);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(100, 38);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            Color bg = isHovered ? COLOR_INPUT_HOVER : COLOR_INPUT_BG;
            Color border = hasFocus() ? COLOR_ACCENT_BLUE : (isHovered ? COLOR_INPUT_BORDER_HOVER : COLOR_INPUT_BORDER);

            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.setColor(border);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

            if (iconType != FieldIconType.NONE) {
                Color iconCol = hasFocus() ? COLOR_BLUE_GLOW : new Color(100, 116, 139);
                paintFieldVectorIcon(g2, iconType, 12, getHeight() / 2, iconCol);
            }

            if (getText().isEmpty() && !hasFocus()) {
                g2.setFont(FONT_INPUT);
                g2.setColor(COLOR_TEXT_MUTED);
                FontMetrics fm = g2.getFontMetrics();
                int px = (iconType != FieldIconType.NONE) ? 34 : 12;
                int py = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(placeholder, px, py);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static class ModernPasswordField extends JPasswordField {
        private final String placeholder;
        private boolean isHovered = false;
        private boolean isEyeHovered = false;
        private boolean passwordVisible = false;

        public ModernPasswordField(String placeholder) {
            this.placeholder = placeholder;
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setFont(FONT_INPUT);
            setForeground(COLOR_TEXT_HEAD);
            setCaretColor(COLOR_ACCENT_BLUE);
            setEchoChar('•');
            setBorder(new EmptyBorder(0, 12, 0, 36));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { isHovered = false; isEyeHovered = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) {
                    if (e.getX() >= getWidth() - 36) {
                        passwordVisible = !passwordVisible;
                        setEchoChar(passwordVisible ? (char) 0 : '•');
                        repaint();
                    }
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    boolean overEye = e.getX() >= getWidth() - 36;
                    if (overEye != isEyeHovered) {
                        isEyeHovered = overEye;
                        setCursor(Cursor.getPredefinedCursor(overEye ? Cursor.HAND_CURSOR : Cursor.TEXT_CURSOR));
                        repaint();
                    }
                }
            });
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { repaint(); }
                @Override public void focusLost(FocusEvent e) { repaint(); }
            });
        }

        public String getPasswordString() {
            return new String(getPassword());
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(super.getPreferredSize().width, 38);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, 38);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(100, 38);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            Color bg = isHovered ? COLOR_INPUT_HOVER : COLOR_INPUT_BG;
            Color border = hasFocus() ? COLOR_ACCENT_BLUE : (isHovered ? COLOR_INPUT_BORDER_HOVER : COLOR_INPUT_BORDER);

            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.setColor(border);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

            int eyeX = getWidth() - 20;
            int eyeY = getHeight() / 2;
            Color eyeCol = isEyeHovered ? COLOR_TEXT_HEAD : COLOR_TEXT_MUTED;
            paintVectorEye(g2, eyeX, eyeY, eyeCol, passwordVisible);

            if (getPassword().length == 0 && !hasFocus()) {
                g2.setFont(FONT_INPUT);
                g2.setColor(COLOR_TEXT_MUTED);
                FontMetrics fm = g2.getFontMetrics();
                int px = 12;
                int py = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(placeholder, px, py);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static void paintFieldVectorIcon(Graphics2D g2, FieldIconType type, int x, int cy, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (type) {
            case USER:

                g2.drawOval(x + 4, cy - 8, 7, 7);
                g2.drawArc(x + 1, cy - 1, 13, 10, 0, 180);
                break;

            case LOCK:

                g2.drawRoundRect(x + 2, cy - 2, 11, 8, 2, 2);
                g2.drawArc(x + 4, cy - 7, 7, 7, 0, 180);
                g2.drawLine(x + 4, cy - 4, x + 4, cy - 2);
                g2.drawLine(x + 11, cy - 4, x + 11, cy - 2);
                break;

            case SHIELD:

                Path2D shield = new Path2D.Float();
                shield.moveTo(x + 2, cy - 6);
                shield.lineTo(x + 13, cy - 6);
                shield.lineTo(x + 13, cy);
                shield.quadTo(x + 13, cy + 6, x + 7.5f, cy + 8);
                shield.quadTo(x + 2, cy + 6, x + 2, cy);
                shield.closePath();
                g2.draw(shield);
                break;

            case EMAIL:

                g2.drawRoundRect(x + 1, cy - 5, 13, 9, 2, 2);
                g2.drawLine(x + 1, cy - 5, x + 7, cy);
                g2.drawLine(x + 14, cy - 5, x + 7, cy);
                break;

            case KEY:

                g2.drawOval(x + 1, cy - 4, 6, 6);
                g2.drawLine(x + 7, cy - 1, x + 14, cy - 1);
                g2.drawLine(x + 11, cy - 1, x + 11, cy + 2);
                g2.drawLine(x + 14, cy - 1, x + 14, cy + 2);
                break;

            case TOKEN:

                g2.drawRoundRect(x + 1, cy - 5, 13, 9, 3, 3);
                g2.drawLine(x + 4, cy, x + 11, cy);
                break;

            case CHAT:

                g2.drawRoundRect(x + 1, cy - 6, 13, 9, 3, 3);
                g2.drawLine(x + 3, cy + 3, x + 2, cy + 6);
                g2.drawLine(x + 2, cy + 6, x + 6, cy + 3);
                break;

            case DOWNLOAD:

                g2.drawLine(x + 2, cy + 4, x + 13, cy + 4);
                g2.drawLine(x + 2, cy + 1, x + 2, cy + 4);
                g2.drawLine(x + 13, cy + 1, x + 13, cy + 4);
                g2.drawLine(x + 7, cy - 5, x + 7, cy + 1);
                g2.drawLine(x + 5, cy - 1, x + 7, cy + 1);
                g2.drawLine(x + 10, cy - 1, x + 7, cy + 1);
                break;

            case WEBHOOK:

                Path2D bolt = new Path2D.Float();
                bolt.moveTo(x + 8, cy - 6);
                bolt.lineTo(x + 4, cy);
                bolt.lineTo(x + 8, cy);
                bolt.lineTo(x + 7, cy + 6);
                bolt.lineTo(x + 12, cy - 1);
                bolt.lineTo(x + 8, cy - 1);
                bolt.closePath();
                g2.draw(bolt);
                break;

            case NONE:
            default:
                break;
        }
    }

    private static void paintVectorEye(Graphics2D g2, int cx, int cy, Color color, boolean visible) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        Path2D eye = new Path2D.Float();
        eye.moveTo(cx - 7, cy);
        eye.quadTo(cx, cy - 5, cx + 7, cy);
        eye.quadTo(cx, cy + 5, cx - 7, cy);
        g2.draw(eye);

        g2.fillOval(cx - 2, cy - 2, 4, 4);

        if (visible) {
            g2.drawLine(cx - 7, cy + 5, cx + 7, cy - 5);
        }
    }

    public static class ModernButton extends JButton {
        private boolean isHovered = false;
        private boolean isPressed = false;
        private boolean isSelectedState = false;

        private Color colorNormal;
        private Color colorHover;
        private Color colorPressed;
        private Color colorBorder;
        private int cornerRadius = 8;
        private boolean isLink = false;
        private boolean isNav = false;
        private boolean isSegmentTab = false;
        private boolean isVectorTitle = false;
        private boolean isCloseTitle = false;

        public static ModernButton primaryButton(String text, ActionListener al) {
            ModernButton b = new ModernButton(text);
            b.setFont(new Font("Segoe UI", Font.BOLD, 13));
            b.setForeground(Color.WHITE);
            b.colorNormal = COLOR_BLUE_PRIMARY;
            b.colorHover = COLOR_ACCENT_BLUE;
            b.colorPressed = new Color(29, 78, 216);
            b.colorBorder = null;
            b.addActionListener(al);
            return b;
        }

        public static ModernButton ghostButton(String text, ActionListener al) {
            ModernButton b = new ModernButton(text);
            b.setFont(new Font("Segoe UI", Font.BOLD, 12));
            b.setForeground(COLOR_TEXT_HEAD);
            b.colorNormal = new Color(20, 22, 30);
            b.colorHover = new Color(30, 34, 48);
            b.colorPressed = new Color(15, 17, 24);
            b.colorBorder = COLOR_BORDER_SUBTLE;
            b.addActionListener(al);
            return b;
        }

        public static ModernButton ghostDangerButton(String text, ActionListener al) {
            ModernButton b = new ModernButton(text);
            b.setFont(new Font("Segoe UI", Font.BOLD, 12));
            b.setForeground(COLOR_ERROR);
            b.colorNormal = new Color(20, 22, 30);
            b.colorHover = new Color(42, 17, 24);
            b.colorPressed = new Color(15, 17, 24);
            b.colorBorder = COLOR_BORDER_SUBTLE;
            b.addActionListener(al);
            return b;
        }

        public static ModernButton dangerButton(String text, ActionListener al) {
            ModernButton b = new ModernButton(text);
            b.setFont(new Font("Segoe UI", Font.BOLD, 13));
            b.setForeground(Color.WHITE);
            b.colorNormal = COLOR_DANGER;
            b.colorHover = new Color(239, 68, 68);
            b.colorPressed = new Color(185, 28, 28);
            b.colorBorder = null;
            b.addActionListener(al);
            return b;
        }

        public static ModernButton linkButton(String text, ActionListener al) {
            ModernButton b = new ModernButton(text);
            b.setFont(FONT_BODY);
            b.setForeground(COLOR_TEXT_SEC);
            b.isLink = true;
            b.addActionListener(al);
            return b;
        }

        public static ModernButton segmentedTab(String text, boolean active, ActionListener al) {
            ModernButton b = new ModernButton(text);
            b.setFont(new Font("Segoe UI", Font.BOLD, 11));
            b.isSegmentTab = true;
            b.isSelectedState = active;
            b.colorNormal = new Color(10, 13, 23);
            b.colorHover = new Color(23, 28, 43);
            b.colorPressed = COLOR_BLUE_PRIMARY;
            b.cornerRadius = 6;
            b.setForeground(active ? Color.WHITE : new Color(143, 155, 179));
            b.addActionListener(al);
            return b;
        }

        public static ModernButton sidebarNavButton(String text, boolean active, ActionListener al) {
            ModernButton b = new ModernButton(text);
            b.setFont(new Font("Segoe UI", Font.BOLD, 12));
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.isNav = true;
            b.isSelectedState = active;
            b.cornerRadius = 8;
            b.colorNormal = COLOR_CARD_HEADER;
            b.colorHover = new Color(24, 27, 36);
            b.colorPressed = new Color(30, 41, 59);
            b.setForeground(active ? COLOR_BLUE_GLOW : COLOR_TEXT_SEC);
            b.setMaximumSize(new Dimension(206, 38));
            b.setPreferredSize(new Dimension(206, 38));
            b.setBorder(new EmptyBorder(0, 14, 0, 14));
            b.addActionListener(al);
            return b;
        }

        public static ModernButton vectorTitleButton(boolean isClose, ActionListener al) {
            ModernButton b = new ModernButton("");
            b.isVectorTitle = true;
            b.isCloseTitle = isClose;
            b.colorNormal = COLOR_CARD_HEADER;
            b.colorHover = isClose ? COLOR_ERROR : new Color(31, 36, 51);
            b.colorPressed = isClose ? new Color(185, 28, 28) : new Color(20, 24, 34);
            b.setPreferredSize(new Dimension(36, 30));
            b.addActionListener(al);
            return b;
        }

        public ModernButton(String text) {
            super(text);
            setOpaque(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.LEFT_ALIGNMENT);

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) { isPressed = true; repaint(); }
                @Override public void mouseReleased(MouseEvent e) { isPressed = false; repaint(); }
            });
        }

        public void setSelectedTab(boolean active) {
            this.isSelectedState = active;
            setForeground(active ? Color.WHITE : new Color(143, 155, 179));
            repaint();
        }

        public void setSelectedNav(boolean active) {
            this.isSelectedState = active;
            setForeground(active ? COLOR_BLUE_GLOW : COLOR_TEXT_SEC);
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            if (isVectorTitle) {
                if (isHovered) {
                    g2.setColor(colorHover);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                }
                g2.setColor(isHovered ? Color.WHITE : COLOR_TEXT_MUTED);
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;

                if (isCloseTitle) {
                    g2.drawLine(cx - 4, cy - 4, cx + 4, cy + 4);
                    g2.drawLine(cx + 4, cy - 4, cx - 4, cy + 4);
                } else {
                    g2.drawLine(cx - 5, cy, cx + 5, cy);
                }
                g2.dispose();
                return;
            }

            if (isLink) {
                g2.setFont(getFont());
                g2.setColor(isHovered ? Color.WHITE : COLOR_TEXT_SEC);
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), x, y);
                if (isHovered) {
                    g2.drawLine(x, y + 2, x + fm.stringWidth(getText()), y + 2);
                }
                g2.dispose();
                return;
            }

            if (isSegmentTab) {
                if (isSelectedState) {
                    g2.setColor(COLOR_ACCENT_BLUE);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
                } else if (isHovered) {
                    g2.setColor(colorHover);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
                }
            } else if (isNav) {
                if (isSelectedState) {
                    g2.setColor(new Color(30, 41, 59));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
                    g2.setColor(COLOR_ACCENT_BLUE);
                    g2.fillRoundRect(0, 8, 3, getHeight() - 16, 2, 2);
                } else if (isHovered) {
                    g2.setColor(colorHover);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
                }
            } else {
                Color fill = isPressed ? colorPressed : (isHovered ? colorHover : colorNormal);
                if (fill != null) {
                    g2.setColor(fill);
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
                }
                if (colorBorder != null) {
                    g2.setColor(colorBorder);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
                }
            }

            g2.setFont(getFont());
            g2.setColor(getForeground());
            FontMetrics fm = g2.getFontMetrics();
            int tx = (getHorizontalAlignment() == SwingConstants.LEFT) ? 14 : (getWidth() - fm.stringWidth(getText())) / 2;
            int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), tx, ty);

            g2.dispose();
        }
    }

    private static class ModernScrollBarUI extends BasicScrollBarUI {
        @Override protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
        @Override protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }

        private JButton createZeroButton() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(COLOR_BG_DARK);
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isThumbRollover() ? new Color(59, 130, 246, 120) : new Color(38, 38, 43));
            int arc = Math.min(r.width, 6);
            g2.fillRoundRect(r.x + 1, r.y + 2, Math.max(r.width - 2, 3), r.height - 4, arc, arc);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
