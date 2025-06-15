import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.JFileChooser;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import java.awt.*;
import java.awt.event.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class Main {
    public static class MapProcessor {
        private final Map<String, Double> dataMap = new HashMap<>();
        public void addOrUpdate(String str, double num) {
            dataMap.merge(str, num, Double::sum);
        }
        public Object[][] getData() {
            return dataMap.entrySet().stream()
                    .map(entry -> new Object[]{entry.getKey(), entry.getValue()})
                    .toArray(Object[][]::new);
        }
    }
    public static class PieChartPanelSectors extends JPanel {
        private Object[][] valuesAndSectors;
        private final Map<String, Color> colorMap = new HashMap<>();
        public PieChartPanelSectors(Object[][] valuesAndSectors) {
            this.valuesAndSectors = valuesAndSectors;
        }
        public void updateData(Object[][] newValues) {
            this.valuesAndSectors = newValues;
            for (Object[] row : valuesAndSectors) {
                String label = (String) row[0];
                if (!colorMap.containsKey(label)) {
                    Color color = new Color(new Random().nextInt(156) + 50,
                            new Random().nextInt(156) + 50,
                            new Random().nextInt(156) + 50);
                    colorMap.put(label, color);
                }
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            setBorder(new LineBorder(Color.black));
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            String[] labels = Arrays.stream(valuesAndSectors)
                    .map(row -> (String) row[0])
                    .toArray(String[]::new);
            double[] values = Arrays.stream(valuesAndSectors)
                    .mapToDouble(row -> (Double) row[1])
                    .toArray();

            int width = getWidth();
            int height = getHeight();
            int diameter = Math.min(width, height) - 40; // Диаметр круга
            int x = width - 210; // Центрирование по X
            int y = (height - diameter) / 2; // Центрирование по Y

            // Считаем общую сумму для вычисления углов
            double total = 0;
            for (double value : values) {
                total += value;
            }

            // Рисуем сектора
            double startAngle = 0;
            for (int i = 0; i < values.length; i++) {
                g2d.setColor(colorMap.get(labels[i]));

                double extent = 360 * (values[i] / total); // Угол сектора
                g2d.fill(new Arc2D.Double(x, y, diameter, diameter, startAngle, extent, Arc2D.PIE));

                // Рисуем легенду
                int legendX = 20;
                int legendY = 20 + i * 25;
                g2d.fillRect(legendX, legendY, 15, 15);
                g2d.setColor(Color.BLACK);
                g2d.drawString(STR."\{labels[i]} (\{String.format("%.3f", 100 * values[i] / paperInfo.finalSum)}%)", legendX + 20, legendY + 12);

                startAngle += extent; // Переходим к следующему сектору
            }
        }
    }
    public static class PieChartPanelTypes extends JPanel {
        private final double[] values;
        private final String[] labels;
        private final Color[] colors;
        public PieChartPanelTypes(double[] values, String[] labels, Color[] colors) {
            this.values = values;
            this.labels = labels;
            this.colors = colors;
        }

        @Override
        protected void paintComponent(Graphics g) {
            setBorder(new LineBorder(Color.black));
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            int width = getWidth();
            int height = getHeight();
            int diameter = Math.min(width, height) - 40; // Диаметр круга
            int x = (width - diameter) / 2; // Центрирование по X
            int y = (height - diameter) / 2; // Центрирование по Y

            // Считаем общую сумму для вычисления углов
            double total = 0;
            for (double value : values) {
                total += value;
            }

            // Рисуем сектора
            double startAngle = 0;
            for (int i = 0; i < values.length; i++) {
                double extent = 360 * (values[i] / total); // Угол сектора
                g2d.setColor(colors[i]);
                g2d.fill(new Arc2D.Double(x, y, diameter, diameter, startAngle, extent, Arc2D.PIE));

                // Рисуем легенду (прямоугольники с цветами)
                int legendX = 20;
                int legendY = 20 + i * 25;
                g2d.setColor(colors[i]);
                g2d.fillRect(legendX, legendY, 15, 15);
                g2d.setColor(Color.BLACK);
                g2d.drawString(STR."\{labels[i]} (\{String.format("%.3f", 100 * values[i] / paperInfo.finalSum)}%)", legendX + 20, legendY + 12);

                startAngle += extent; // Переходим к следующему сектору
            }
        }
    }
    public static class ComboListener extends KeyAdapter {
        @SuppressWarnings("rawtypes")
        JComboBox cbListener;
        @SuppressWarnings("rawtypes")
        Vector vector;
        @SuppressWarnings("rawtypes")
        public ComboListener(JComboBox cbListenerParam, Vector vectorParam)
        {
            cbListener = cbListenerParam;
            vector = vectorParam;
        }
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public void keyTyped(KeyEvent key)
        {
            String text = ((JTextField)key.getSource()).getText();
            cbListener.setModel(new DefaultComboBoxModel(getFilteredList(text)));
            cbListener.setSelectedIndex(-1);
            ((JTextField)cbListener.getEditor().getEditorComponent()).setText(text);
            cbListener.showPopup();
        }
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public Vector getFilteredList(String text)
        {
            Vector v = new Vector();
            for (Object o : vector) {
                if (o.toString().contains(text.toLowerCase()) || o.toString().contains(text.toUpperCase())) {
                    v.add(o.toString());
                }
            }
            return v;
        }
    }
    public static Color mainColor1 = new Color(240, 240, 240);
    public static Color mainColor2 = new Color(204, 204, 204);
    public static Font mainFont = new Font("Segoe UI", Font.BOLD, 16);
    public static class GraphPanel extends JPanel {
        private final double[] values;
        private final String[] dates;
        private final int countOfPoints;
        private double minValue;
        private double maxValue;
        private int hoveredIndex = -1;
        public GraphPanel(double[] values, String[] dates, int countOfPoints) {
            if (countOfPoints == 0) {
                this.countOfPoints = values.length;
            } else {
                this.countOfPoints = countOfPoints;
            }
            this.values = Arrays.copyOf(values, this.countOfPoints);
            this.dates = Arrays.copyOf(dates, this.countOfPoints);

            // Находим мин/макс для масштабирования
            this.minValue = Double.MAX_VALUE;
            this.maxValue = -Double.MAX_VALUE;
            for (double v : this.values) {
                if (v < minValue) minValue = v;
                if (v > maxValue) maxValue = v;
            }
            setBackground(Color.WHITE);

            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int oldHoveredIndex = hoveredIndex;
                    hoveredIndex = findClosestPointIndex(e.getX(), e.getY());
                    if (hoveredIndex != oldHoveredIndex) {
                        repaint();  // Перерисовываем график
                    }
                }
            });
        }
        // Находит ближайшую точку графика к координатам мыши
        private int findClosestPointIndex(int mouseX, int mouseY) {
            int width = getWidth()-10;
            int height = getHeight();
            int westEastPadding = 30;
            int padding = 30;
            double xStep = (double) (width - 2 * westEastPadding) / (countOfPoints - 1);
            double yScale = (height - 2 * padding) / (maxValue - minValue);

            int closestIndex = -1;
            double minDistance = Double.MAX_VALUE;

            for (int i = 0; i < countOfPoints; i++) {
                double x = westEastPadding + (countOfPoints - 1 - i) * xStep;
                double y = height - padding - (values[i] - minValue) * yScale;
                double distance = Math.sqrt(Math.pow(mouseX - x, 2) + Math.pow(mouseY - y, 2));

                if (distance < 9 && distance < minDistance) {
                    minDistance = distance;
                    closestIndex = i;
                }
            }
            return closestIndex;
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth()-10;
            int height = getHeight();
            int westEastPadding = 25;
            int padding = 25;

            // Оси координат
            g2d.setColor(Color.BLACK);
            g2d.drawLine(westEastPadding, height - padding, width - westEastPadding, height - padding); // Ось X
            g2d.drawLine(westEastPadding, height - padding, westEastPadding, padding); // Ось Y

            // Подписи осей
            g2d.drawString("Дата", width - 25, height - padding);
            g2d.drawString("Цена", padding, 20);

            // Масштабируем значения под размер панели
            double xStep = (double) (width - 2 * westEastPadding) / (countOfPoints - 1);
            double yScale = (height - 2 * padding) / (maxValue - minValue);

            // Подписи значений на оси X (абсциссы)
            g2d.setColor(Color.BLACK);
            int xLabelFrequency = Math.max(1, countOfPoints / 7); // Частота подписей, чтобы не было нагромождения
            for (int i = 0; i < countOfPoints; i += xLabelFrequency) {
                double x = westEastPadding + (countOfPoints - 1 - i) * xStep;
                String label = dates[i]; // или другой текст, если нужно
                g2d.drawString(label, (int) x, height - padding + 15);
            }

            // Подписи значений на оси Y (ординаты)
            int numYLabels = 2; // Количество подписей на оси Y
            for (int i = 0; i <= numYLabels; i++) {
                double yValue = minValue + (maxValue - minValue) * i / numYLabels;
                int y = height - padding - (int) ((yValue - minValue) * yScale);
                String label = String.format("%.2f", yValue); // Форматирование числа
                g2d.drawString(label, 0, y + 5);
            }

            // Рисуем график
            Path2D path = new Path2D.Double();
            path.moveTo(westEastPadding, height - padding - (values[0] - minValue) * yScale);

            for (int i = 0; i < countOfPoints; i++) {
                double x = westEastPadding + (countOfPoints - 1 - i) * xStep;
                double y = height - padding - (values[i] - minValue) * yScale;
                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }

            g2d.setColor(Color.GREEN);
            g2d.setStroke(new BasicStroke(2));
            g2d.draw(path);

            // Точки на графике
            g2d.setColor(Color.RED);
            for (int i = 0; i < countOfPoints; i++) {
                double x = westEastPadding + (countOfPoints - 1 - i) * xStep;
                double y = height - padding - (values[i] - minValue) * yScale;
                g2d.fillOval((int) x - 3, (int) y - 3, 6, 6);
            }

            // Показываем значение при наведении
            if (hoveredIndex != -1) {
                double x = westEastPadding + (countOfPoints - 1 - hoveredIndex) * xStep;
                double y = height - padding - (values[hoveredIndex] - minValue) * yScale;
                String tooltip = String.format("Дата: %s\nЦена: %.2f", dates[hoveredIndex], values[hoveredIndex]);

                // Рисуем подсказку
                g2d.setColor(new Color(240, 240, 240, 200));  // Полупрозрачный фон
                g2d.fillRect((int) x + 5, (int) y - 25, 150, 30);
                g2d.setColor(Color.BLACK);
                g2d.drawString(tooltip.split("\n")[0], (int) x + 10, (int) y - 10);
                g2d.drawString(tooltip.split("\n")[1], (int) x + 10, (int) y + 5);

                g2d.setColor(new Color(100, 100, 100, 150));
                g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0)); // Пунктир
                g2d.drawLine((int) x, height - padding, (int) x, (int) y); // Вертикальная линия до оси X
                g2d.drawLine(padding, (int) y, (int) x, (int) y); // Горизонтальная линия до оси Y

                // Подписи на осях
                g2d.setColor(Color.BLUE);
                g2d.setStroke(new BasicStroke(1));

                // Подпись на оси X (дата)
                String xLabel = dates[hoveredIndex];
                g2d.drawString(xLabel, (int) x - 20, height - padding + 20);

                // Подпись на оси Y (значение)
                String yLabel = String.format("%.2f", values[hoveredIndex]);
                g2d.drawString(yLabel, padding + 3, (int) y - 10);
            }
        }
    }
    public static class defaultPanel extends JPanel {
        public defaultPanel() {
            setBackground(Color.white);
            setBorder(new LineBorder(Color.black));
        }
    }
    public static class defaultLabel extends JLabel {
        public defaultLabel(String text) {
            setFont(mainFont);
            setText(text);
            setHorizontalAlignment(CENTER);
            setVerticalAlignment(CENTER);
            setBorder(new LineBorder(Color.black));
        }
    }
    public static class userButton extends JButton { //кнопки в главной панели
        public userButton(String text) {
            setFont(mainFont);
            setMargin(new Insets(0, 0, 0, 0));
            setForeground(Color.black);
            setText(text);
            setFocusPainted(false);
            setBackground(Color.white);
        }
    }
    public static class userPortfolioPaper extends userButton {
        static double sumPrice;
        private static userPortfolioPaper lastSelected = null;
        private final Color defaultColor;
        private final Color selectedColor = Color.yellow;
        private final String paperType;
        private final String economicSector;
        private final double doubPaperPrice;
        private void selectButton() {
            if (lastSelected != null) {
                lastSelected.setBackground(lastSelected.defaultColor);
            }

            this.setBackground(selectedColor);
            lastSelected = this;
        }
        public userPortfolioPaper(String text, String paperNameForSearch, String paperType, String paperPrice, double nominal, int countOfPapers, String economicSector){
            super(text);
            this.paperType = paperType;
            this.economicSector = economicSector;
            this.defaultColor = getBackground();
            this.doubPaperPrice = Double.parseDouble(paperPrice.replace(" руб.", "").replace(" ", ""));
            if (paperType.equals("Акция")) {
                sumPrice = doubPaperPrice * nominal * countOfPapers;
            } else {
                sumPrice = (doubPaperPrice/100) * nominal * countOfPapers;
            }
            setAlignmentX(Component.CENTER_ALIGNMENT);
            setMargin(new Insets(15,0,15,0));
            setMaximumSize(new Dimension(340, 80));
            setMinimumSize(new Dimension(340, 80));
            setPreferredSize(new Dimension(340, 80));

            this.addActionListener(_ -> selectButton());

            addActionListener(_ -> {
                if (getBackground() == Color.white) {
                    setBackground(Color.YELLOW);
                    paperInfo.removeButton.setEnabled(true);
                    if (this.paperType.equals("Акция")) {
                        paperInfo.paperInfoTable.setLayout(new GridLayout(3, 2));
                        paperInfo.paperInfoTable.remove(paperInfo.emptyTableFiller);
                        paperInfo.paperInfoTable.remove(paperInfo.paperISIN);
                        paperInfo.paperInfoTable.remove(paperInfo.paperType);
                        paperInfo.paperInfoTable.remove(paperInfo.obligationNominal);
                        paperInfo.paperInfoTable.remove(paperInfo.obligationCoupon);
                        paperInfo.paperInfoTable.remove(paperInfo.NKD);
                        paperInfo.paperInfoTable.remove(paperInfo.creditRating);
                        paperInfo.paperInfoTable.remove(paperInfo.nextCouponDate);
                        paperInfo.paperInfoTable.remove(paperInfo.economicsSector);
                        paperInfo.paperInfoTable.remove(paperInfo.closePrice);
                        paperInfo.paperInfoTable.remove(paperInfo.openPrice);
                        paperInfo.paperInfoTable.remove(paperInfo.actualPrice);
                        paperInfo.paperInfoTable.remove(paperInfo.priceChange);

                        paperInfo.paperInfoTable.add(paperInfo.paperISIN);
                        paperInfo.paperInfoTable.add(paperInfo.paperType);
                        paperInfo.paperInfoTable.add(paperInfo.closePrice);
                        paperInfo.paperInfoTable.add(paperInfo.openPrice);
                        paperInfo.paperInfoTable.add(paperInfo.actualPrice);
                        paperInfo.paperInfoTable.add(paperInfo.priceChange);

                        Element hrefElement = papers.actionInfoSite_02.select(STR."a:containsOwn(\{paperNameForSearch})").first();
                        assert hrefElement != null;
                        String href = hrefElement.attr("href");
                        try {
                            Document tempHref = Jsoup.connect(STR."https://mfd.ru\{href}").userAgent("Mozilla/5.0 " +
                                    "(Windows NT 10.0; Win64; x64) " +
                                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                    "Chrome/58.0.3029.110 Safari/537.36").get();
                            Element tempName = tempHref.getElementsByClass("m-companytable-last").getFirst();
                            Elements tds = tempHref.select("tr td");
                            for (Element td : tds) {
                                if (td.text().equals("Пред закр")) {
                                    paperInfo.closePrice.setText(STR."Закрытие: \{Objects.requireNonNull(td.nextElementSibling()).text()}");
                                }
                                if (td.text().equals("Открытие")) {
                                    paperInfo.openPrice.setText(STR."Открытие: \{Objects.requireNonNull(td.nextElementSibling()).text()}");
                                }
                                if (td.text().equals("Изм")) {
                                    paperInfo.priceChange.setText(STR."Изм. за день: \{Objects.requireNonNull(td.nextElementSibling()).text()}");
                                }
                            }
                            assert tempName != null;
                            String actionTemp = tempName.text();
                            paperInfo.actualPrice.setText(STR."Цена: \{actionTemp} руб.");
                            paperInfo.paperName.setText(paperNameForSearch);
                            paperInfo.paperType.setText("Акция");

                            String paperTicker = Jsoup.parse(this.getText()).getElementsByTag("td").getFirst().text().split(" ", 2)[0].replace("#", "");
                            Document siteForEconomicSector = Jsoup.connect(STR."https://ru.tradingview.com/symbols/RUS-\{paperTicker}/").get();
                            paperInfo.paperISIN.setText(STR."Сектор: \{siteForEconomicSector.select("div.content-e86I10sk").select("div.apply-overflow-tooltip.value-QCJM7wcY").getFirst().text()}");
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                    } else {
                        paperInfo.paperInfoTable.setLayout(new GridLayout(6, 2));
                        paperInfo.paperInfoTable.remove(paperInfo.emptyTableFiller);
                        paperInfo.paperInfoTable.remove(paperInfo.paperISIN);
                        paperInfo.paperInfoTable.remove(paperInfo.paperType);
                        paperInfo.paperInfoTable.remove(paperInfo.closePrice);
                        paperInfo.paperInfoTable.remove(paperInfo.openPrice);
                        paperInfo.paperInfoTable.remove(paperInfo.actualPrice);
                        paperInfo.paperInfoTable.remove(paperInfo.priceChange);
                        paperInfo.paperInfoTable.add(paperInfo.paperISIN);
                        paperInfo.paperInfoTable.add(paperInfo.paperType);
                        paperInfo.paperInfoTable.add(paperInfo.obligationNominal);
                        paperInfo.paperInfoTable.add(paperInfo.obligationCoupon);
                        paperInfo.paperInfoTable.add(paperInfo.NKD);
                        paperInfo.paperInfoTable.add(paperInfo.creditRating);
                        paperInfo.paperInfoTable.add(paperInfo.nextCouponDate);
                        paperInfo.paperInfoTable.add(paperInfo.economicsSector);
                        paperInfo.paperInfoTable.add(paperInfo.closePrice);
                        paperInfo.paperInfoTable.add(paperInfo.openPrice);
                        paperInfo.paperInfoTable.add(paperInfo.actualPrice);
                        paperInfo.paperInfoTable.add(paperInfo.priceChange);

                        Element hrefElement = papers.obligationInfoSite_01.select(STR."a:containsOwn(\{paperNameForSearch})").first();
                        assert hrefElement != null;
                        String href = hrefElement.attr("href");
                        try {
                            Document tempHref = Jsoup.connect(STR."https://mfd.ru\{href}").userAgent("Mozilla/5.0 " +
                                    "(Windows NT 10.0; Win64; x64) " +
                                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                    "Chrome/58.0.3029.110 Safari/537.36").get();
                            Element obligationCode = tempHref.getElementsByTag("h1").first();
                            assert obligationCode != null;

                            paperInfo.paperISIN.setText(STR."ISIN: \{STR."RU00\{obligationCode.text().split("RU00|:")[1].trim()}"}");
                            Elements tds = tempHref.select("tr td");
                            for (Element td : tds) {
                                if (td.text().equals("Пред закр")) {
                                    paperInfo.closePrice.setText(STR."Закрытие: \{Objects.requireNonNull(td.nextElementSibling()).text()}%");
                                }
                                if (td.text().equals("Открытие")) {
                                    paperInfo.openPrice.setText(STR."Открытие: \{Objects.requireNonNull(td.nextElementSibling()).text()}%");
                                }
                            }
                            driver.get(STR."https://smart-lab.ru/q/bonds/\{STR."RU00\{obligationCode.text().split("RU00|:")[1].trim()}"}");
                            Document obligationDataSite = Jsoup.parse(driver.getPageSource());

                            paperInfo.NKD.setText(STR."НКД: \{obligationDataSite.select("div[title='Накопленный купонный доход']")
                                    .select("div").get(2).text()}");

                            Elements divs = obligationDataSite.select("div.quotes-simple-table__item");
                            String currency =" ";
                            for (Element div : divs) {
                                if (div.text().equals("Котировка облигации, %")) {
                                    paperInfo.actualPrice.setText(STR."Цена: \{Objects.requireNonNull(div.nextElementSibling()).text()}");
                                }
                                if (div.text().equals("Изм за день, %")) {
                                    paperInfo.priceChange.setText(STR."Изм. за день: \{Objects.requireNonNull(div.nextElementSibling()).text()}");
                                }
                                if (div.text().equals("Валюта")) {
                                    currency = Objects.requireNonNull(div.nextElementSibling()).text();
                                }
                                if (div.text().equals("Номинал")) {
                                    paperInfo.obligationNominal.setText(STR."Номинал: \{Objects.requireNonNull(div.nextElementSibling()).text()}");
                                }
                                if (div.text().equals("Ставка купона")) {
                                    paperInfo.obligationCoupon.setText(STR."Купон: \{Objects.requireNonNull(div.nextElementSibling()).text()}");
                                }
                                if (div.text().equals("Дата след. выплаты")) {
                                    paperInfo.nextCouponDate.setText(STR."След. выплата: \{Objects.requireNonNull(div.nextElementSibling()).text()}");
                                }
                                if (div.text().equals("Сектор")) {
                                    paperInfo.economicsSector.setText(STR."Сектор: \{Objects.requireNonNull(div.nextElementSibling()).text()}");
                                }
                            }
                            paperInfo.creditRating.setText(STR."Кред. рейтинг: \{obligationDataSite.getElementsByClass("linear-progress-bar__text").text()}");
                            if (paperInfo.creditRating.getText().equals("Кред. рейтинг: ")) paperInfo.creditRating.setText("В процессе присвоения");
                            if (paperInfo.economicsSector.getText().equals("Государственные")) paperInfo.creditRating.setText("Государственная облигация");
                            if (paperInfo.economicsSector.getText().equals("Субфедеральные")) paperInfo.creditRating.setText("Субфед. облигация");
                            if (currency.equals("SUR")) {
                                paperInfo.obligationNominal.setText(STR."\{paperInfo.obligationNominal.getText()} RUB");
                            } else {
                                paperInfo.obligationNominal.setText(STR."\{paperInfo.obligationNominal.getText()} \{currency}");
                            }
                            paperInfo.paperName.setText(paperNameForSearch);
                            paperInfo.paperType.setText("Облигация");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else {
                    setBackground(Color.white);
                    paperInfo.removeButton.setEnabled(false);
                }
            });
        }
    }
    public static class mainFrame extends JFrame { //окно
        papers sharedPapers = new papers();
        portfolioData userPortfolioData;
        analyticsData analyticsData;
        tradingStrategies tradingStrategies;
        papersOperations papersOperations;
        public mainFrame() throws IOException {
            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    driver.quit();
                    dispose();
                    System.exit(0);
                }
            });
            setSize(1280, 720);
            getContentPane().setBackground(mainColor1);
            setLayout(null);
            setResizable(false);
            setTitle("Invest demo");
            userPortfolioData = new portfolioData(sharedPapers);
            analyticsData = new analyticsData(sharedPapers);
            tradingStrategies = new tradingStrategies();
            papersOperations = new papersOperations(sharedPapers);

            menuPanel menuPanel = new menuPanel(userPortfolioData, analyticsData, tradingStrategies, papersOperations);
            add(menuPanel);
            add(userPortfolioData);
            add(analyticsData);
            add(tradingStrategies);
            add(papersOperations);

            JDialog agreementDialog = new JDialog(this, "Соглашение", true);
            agreementDialog.setLayout(new BorderLayout());
            agreementDialog.setSize(400, 500);
            agreementDialog.setLocationRelativeTo(this);

            JPanel checkBoxPanel = new JPanel(new GridLayout(4, 1));

            JLabel agreementLabel = new JLabel("<html><div style='text-align: center;'>Перед использованием приложения вы должны принять следующие положения:</div></html>");
            agreementLabel.setFont(mainFont);

            JCheckBox check1 = new JCheckBox("<html><div style='text-align: left;'>Все отображаемые в приложении данные принадлежат ПАО Московская биржа</div></html>");
            check1.setFont(mainFont);
            check1.setFocusPainted(false);

            JCheckBox check2 = new JCheckBox("<html><div style='text-align: left;'>Я обязусь не демонстрировать третьим лицам полученные данные и" +
                    " полученные после их обработки приложением производные данные</div></html>");
            check2.setFont(mainFont);
            check2.setFocusPainted(false);

            JCheckBox check3 = new JCheckBox("<html><div style='text-align: left;'>Я осознаю все риски торговли ценными бумагами и признаю, что информация, " +
                    "отображаемая в приложении, не является индивидуальной инвестиционной рекомендацией</div></html>");
            check3.setFont(mainFont);
            check3.setFocusPainted(false);

            checkBoxPanel.add(agreementLabel);
            checkBoxPanel.add(check3);
            checkBoxPanel.add(check2);
            checkBoxPanel.add(check1);

            userButton acceptButton = new userButton("Принимаю");
            acceptButton.setEnabled(false);

            ItemListener checkBoxListener = _ -> acceptButton.setEnabled(check1.isSelected() && check2.isSelected() && check3.isSelected());

            check1.addItemListener(checkBoxListener);
            check2.addItemListener(checkBoxListener);
            check3.addItemListener(checkBoxListener);

            acceptButton.addActionListener(_ -> agreementDialog.dispose());

            agreementDialog.add(checkBoxPanel, BorderLayout.CENTER);
            agreementDialog.add(acceptButton, BorderLayout.SOUTH);

            agreementDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            agreementDialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    driver.quit();
                    agreementDialog.dispose();
                    System.exit(0);
                }
            });

            agreementDialog.setVisible(true);
        }
    }
    public static class menuPanel extends JPanel { //главная панель меню
        userButton userPortfolioButton = new userButton("Портфель");
        userButton marketAnalyticsButton = new userButton("<html><center>Аналитика<br>рынка</html>");
        userButton tradingStrategiesButton = new userButton("<html><center>Торговые<br>стратегии</html>");
        userButton paperOperationsButton = new userButton("<html><center>Операции с<br>активами</html>");
        settingsPanel settingsPanel = new settingsPanel();
        public menuPanel(JPanel userPortfolioData, JPanel marketAnalyticsData, JPanel tradingStrategiesData, JPanel paperOperationsPanel) {
            setSize(120, 682);
            setLocation(0, 0);
            setBackground(mainColor2);
            setBorder(new CompoundBorder(new LineBorder(Color.BLACK), new EmptyBorder(10, 10, 10, 10)));
            setLayout(new GridLayout(0, 1, 15, 15));
            userPortfolioButton.setEnabled(false);

            add(userPortfolioButton);
            add(marketAnalyticsButton);
            add(paperOperationsButton);
            add(tradingStrategiesButton);
            add(settingsPanel);

            userPortfolioButton.addActionListener(_ -> {
                userPortfolioData.setVisible(true);
                marketAnalyticsData.setVisible(false);
                tradingStrategiesData.setVisible(false);
                paperOperationsPanel.setVisible(false);

                userPortfolioButton.setEnabled(false);
                marketAnalyticsButton.setEnabled(true);
                tradingStrategiesButton.setEnabled(true);
                paperOperationsButton.setEnabled(true);
            });
            marketAnalyticsButton.addActionListener(_ -> {
                userPortfolioData.setVisible(false);
                marketAnalyticsData.setVisible(true);
                tradingStrategiesData.setVisible(false);
                paperOperationsPanel.setVisible(false);

                userPortfolioButton.setEnabled(true);
                marketAnalyticsButton.setEnabled(false);
                tradingStrategiesButton.setEnabled(true);
                paperOperationsButton.setEnabled(true);
            });
            tradingStrategiesButton.addActionListener(_ -> {
                userPortfolioData.setVisible(false);
                marketAnalyticsData.setVisible(false);
                tradingStrategiesData.setVisible(true);
                paperOperationsPanel.setVisible(false);

                userPortfolioButton.setEnabled(true);
                marketAnalyticsButton.setEnabled(true);
                tradingStrategiesButton.setEnabled(false);
                paperOperationsButton.setEnabled(true);
            });
            paperOperationsButton.addActionListener(_ -> {
                userPortfolioData.setVisible(false);
                marketAnalyticsData.setVisible(false);
                tradingStrategiesData.setVisible(false);
                paperOperationsPanel.setVisible(true);

                userPortfolioButton.setEnabled(true);
                marketAnalyticsButton.setEnabled(true);
                tradingStrategiesButton.setEnabled(true);
                paperOperationsButton.setEnabled(false);
            });
        }
    }
    public static class settingsPanel extends JPanel { //панель с настройками
        userButton mainSettings = new userButton("Настройки");
        userButton questions = new userButton("Помощь");

        public settingsPanel() {
            setBackground(mainColor2);
            setLayout(new GridLayout(2, 0, 3, 10));
            add(mainSettings);
            add(questions);
        }
    }
    public static ChromeOptions options = new ChromeOptions();
    public static WebDriver driver;
    public static class papers extends JPanel implements paperInfo.ButtonClickListener { //панель с поиском бумаг
        MapProcessor processor = new MapProcessor();
        JPanel papersButtons = new JPanel();
        static Document actionsInfoSite_01;
        static {
            try {
                driver.get("https://smart-lab.ru/q/shares/");
                actionsInfoSite_01 = Jsoup.parse(driver.getPageSource());
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        static Document actionInfoSite_02;
        static {
            try {
                actionInfoSite_02 = Jsoup.connect("https://mfd.ru/marketdata/?id=5&mode=3&group=16")
                        .userAgent("Mozilla/5.0 " +
                        "(Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/91.0.4472.124 YaBrowser/25.4.0.0 Safari/537.36")
                        .get();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        static Document obligationInfoSite_01;
        static {
            try {
                obligationInfoSite_01 = Jsoup.connect("https://mfd.ru/marketdata/?id=5&mode=3&group=17").userAgent("Mozilla/5.0 " +
                        "(Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/91.0.4472.124 YaBrowser/25.4.0.0 Safari/537.36")
                        .get();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Elements actionNamesByClass = actionsInfoSite_01.getElementsByClass("trades-table__name");
        Elements obligationNamesByClass = Objects.requireNonNull(obligationInfoSite_01.getElementById("mfd-instruments-selected")).select("option");
        ArrayList<String> actionNamesAll = new ArrayList<>();
        ArrayList<String> obligationNamesAll = new ArrayList<>();
        String[] actionTickersAll = actionsInfoSite_01.getElementsByClass("trades-table__ticker").text().split(" ");
        String[] actionNames;
        String[] obligationNames;
        String[] actionTickers = Arrays.copyOfRange(actionTickersAll, 2, actionTickersAll.length);
        JComboBox<String> comboBox = new JComboBox<>();
        userButton action = new userButton("    Акции    ");
        userButton obligation = new userButton("Облигации");
        private final JPanel papersList = new JPanel() {
            {
                setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
                add(Box.createRigidArea(new Dimension(3, 10)));
                setBackground(mainColor2);
            }
        };
        JScrollPane scrollPane = new JScrollPane(papersList) {
            {
                setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            }
        };
        Vector<String> vector = new Vector<>();
        JTextField textField = (JTextField) comboBox.getEditor().getEditorComponent();
        String obligationISIN;
        String receivedItem;
        private final JPanel papersListCopy_1 = new JPanel() {
            {
                setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
                add(Box.createRigidArea(new Dimension(3, 10)));
                setBackground(mainColor2);
            }
        };
        private final JPanel papersListCopy_2 = new JPanel() {
            {
                setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
                add(Box.createRigidArea(new Dimension(3, 10)));
                setBackground(mainColor2);
            }
        };
        @Override
        public void onButtonClicked(Object[] data) {
            papersList.add(new userPortfolioPaper((String) data[0], (String) data[1], (String) data[2],
                    (String) data[3], (double) data[4], (Integer) data[5], (String) data[6]));
            papersList.add(Box.createRigidArea(new Dimension(3, 10)));
            papersListCopy_1.add(new userPortfolioPaper((String) data[0], (String) data[1], (String) data[2],
                    (String) data[3], (double) data[4], (Integer) data[5], (String) data[6]));
            papersListCopy_1.add(Box.createRigidArea(new Dimension(3, 10)));
            papersListCopy_2.add(new userPortfolioPaper((String) data[0], (String) data[1], (String) data[2],
                    (String) data[3], (double) data[4], (Integer) data[5], (String) data[6]));
            papersListCopy_2.add(Box.createRigidArea(new Dimension(3, 10)));
        }
        public JPanel getPapersList_1() {
            return papersListCopy_1;
        }
        public JPanel getPapersList_2() {
            return papersListCopy_2;
        }
        public void removePaper(String paperText) {
            for (Component component : papersListCopy_1.getComponents()) {
                if (component instanceof userPortfolioPaper && ((userPortfolioPaper) component).getText().equals(paperText)) {
                    papersListCopy_1.remove(component);
                    break;
                }
            }
            for (Component component : papersListCopy_2.getComponents()) {
                if (component instanceof userPortfolioPaper && ((userPortfolioPaper) component).getText().equals(paperText)) {
                    papersListCopy_2.remove(component);
                    break;
                }
            }
            papersListCopy_1.updateUI();
            papersListCopy_2.updateUI();
        }
        public void repaintTable(boolean is1, boolean is2) {
            paperInfo.paperInfoTable.setLayout(new GridLayout(1, 1));
            paperInfo.paperInfoTable.add(paperInfo.emptyTableFiller);
            paperInfo.paperName.setText(" ");
            paperInfo.paperInfoTable.remove(paperInfo.paperISIN);
            paperInfo.paperInfoTable.remove(paperInfo.paperType);
            paperInfo.paperInfoTable.remove(paperInfo.closePrice);
            paperInfo.paperInfoTable.remove(paperInfo.openPrice);
            paperInfo.paperInfoTable.remove(paperInfo.actualPrice);
            paperInfo.paperInfoTable.remove(paperInfo.priceChange);
            paperInfo.paperInfoTable.remove(paperInfo.obligationNominal);
            paperInfo.paperInfoTable.remove(paperInfo.obligationCoupon);
            paperInfo.paperInfoTable.remove(paperInfo.NKD);
            paperInfo.paperInfoTable.remove(paperInfo.creditRating);
            paperInfo.paperInfoTable.remove(paperInfo.nextCouponDate);
            paperInfo.paperInfoTable.remove(paperInfo.economicsSector);
            paperInfo.paperInfoTable.repaint();

            obligation.setEnabled(is1);
            action.setEnabled(is2);
            paperInfo.paperInfoTable.updateUI();
        }
        public papers() {
            setLayout(new BorderLayout());
            setBorder(new LineBorder(Color.black));
            setSize(375, 660);

            for (Element item : obligationNamesByClass) {
                obligationNamesAll.add(item.text());
            }
            obligationNames = obligationNamesAll.toArray(String[]::new);

            for (Element item : actionNamesByClass) {
                actionNamesAll.add(item.text());
            }
            actionNamesAll.removeFirst();
            actionNamesAll.removeFirst();
            actionNames = actionNamesAll.toArray(String[]::new);
            for (int i = 0; i < actionNames.length; i++) {
                vector.add(STR."#\{actionTickers[i]} \{actionNames[i]}");
            }

            ItemListener forActions = e -> {
                if (comboBox.isDisplayable() && e.getStateChange() == ItemEvent.SELECTED) {
                    paperInfo.paperInfoTable.setLayout(new GridLayout(3, 2));
                    paperInfo.paperInfoTable.remove(paperInfo.emptyTableFiller);
                    paperInfo.paperInfoTable.add(paperInfo.paperISIN);
                    paperInfo.paperInfoTable.add(paperInfo.paperType);
                    paperInfo.paperInfoTable.add(paperInfo.closePrice);
                    paperInfo.paperInfoTable.add(paperInfo.openPrice);
                    paperInfo.paperInfoTable.add(paperInfo.actualPrice);
                    paperInfo.paperInfoTable.add(paperInfo.priceChange);
                    receivedItem = e.getItem().toString().split(" ", 2)[1];
                    Element hrefElement = actionInfoSite_02.select(STR."a:containsOwn(\{receivedItem})").first();
                    assert hrefElement != null;
                    String href = hrefElement.attr("href");
                    try {
                        Document tempHref = Jsoup.connect(STR."https://mfd.ru\{href}").userAgent("Mozilla/5.0 " +
                                "(Windows NT 10.0; Win64; x64) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                "Chrome/58.0.3029.110 Safari/537.36").get();
                        Element tempName = tempHref.getElementsByClass("m-companytable-last").getFirst();
                        Elements tds = tempHref.select("tr td");
                        for (Element td : tds) {
                            if (td.text().equals("Пред закр")) {
                                paperInfo.closePrice.setText(STR."Закрытие: \{Objects.requireNonNull(td.nextElementSibling()).text()}");
                            }
                            if (td.text().equals("Открытие")) {
                                paperInfo.openPrice.setText(STR."Открытие: \{Objects.requireNonNull(td.nextElementSibling()).text()}");
                            }
                            if (td.text().equals("Изм")) {
                                paperInfo.priceChange.setText(STR."Изм. за день: \{Objects.requireNonNull(td.nextElementSibling()).text()}");
                            }
                        }
                        assert tempName != null;
                        String actionTemp = tempName.text();
                        paperInfo.actualPrice.setText(STR."Цена: \{actionTemp} руб.");
                        paperInfo.paperName.setText(e.getItem().toString());
                        paperInfo.paperType.setText("Акция");
                        String paperTicker = e.getItem().toString().split(" ", 2)[0].replace("#", "");
                        //String paperTicker = Jsoup.parse(this.getText()).getElementsByTag("td").getFirst().text().split(" ", 2)[0].replace("#", "");
                        Document siteForEconomicSector = Jsoup.connect(STR."https://ru.tradingview.com/symbols/RUS-\{paperTicker}/").get();
                        paperInfo.paperISIN.setText(STR."Сектор: \{siteForEconomicSector.select("div.content-e86I10sk").select("div.apply-overflow-tooltip.value-QCJM7wcY").getFirst().text()}");
//                        System.out.println(actionTemp);

                        paperInfo.addPaper.setEnabled(true);
                        for (Component component : papersList.getComponents()) {
                            if (component instanceof userPortfolioPaper button) {
                                if (button.getText().equals(e.getItem().toString())) {
                                    paperInfo.addPaper.setEnabled(false);
                                    break;
                                }
                            }
                        }
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                }
            };

            comboBox.setEditable(true);
            comboBox.setSelectedIndex(-1);
            comboBox.setFont(mainFont);
            comboBox.setToolTipText("Введите название или тикер...");
            comboBox.setModel(new DefaultComboBoxModel<>(vector));

            for (ItemListener listener : comboBox.getItemListeners()) {
                comboBox.removeItemListener(listener);
            }
            comboBox.addItemListener(forActions);

            textField.setFocusable(true);
            textField.setText("");
            textField.addKeyListener(new ComboListener(comboBox, vector));

            action.setEnabled(false);
            action.addActionListener(_ -> {
                repaintTable(true, false);
                comboBox.removeAllItems();
                vector.removeAllElements();

                for (int i = 0; i < actionNames.length; i++) {
                    vector.add(STR."#\{actionTickers[i]} \{actionNames[i]}");
                }
                comboBox.setModel(new DefaultComboBoxModel<>(vector));
                comboBox.setSelectedIndex(-1);
                for (ItemListener listener : comboBox.getItemListeners()) {
                    comboBox.removeItemListener(listener);
                }
                comboBox.addItemListener(forActions);
            });

            obligation.addActionListener(_ -> {
                repaintTable(false, true);
                comboBox.removeAllItems();
                vector.removeAllElements();
                vector.addAll(Arrays.asList(obligationNames));
                comboBox.setModel(new DefaultComboBoxModel<>(vector));
                comboBox.setSelectedIndex(-1);
                for (ItemListener listener : comboBox.getItemListeners()) {
                    comboBox.removeItemListener(listener);
                }

                comboBox.addItemListener(e ->{
                    if (comboBox.isDisplayable() && e.getStateChange() == ItemEvent.SELECTED) {
                        paperInfo.paperInfoTable.setLayout(new GridLayout(6, 2));
                        paperInfo.paperInfoTable.remove(paperInfo.emptyTableFiller);
                        paperInfo.paperInfoTable.add(paperInfo.paperISIN);
                        paperInfo.paperInfoTable.add(paperInfo.paperType);
                        paperInfo.paperInfoTable.add(paperInfo.obligationNominal);
                        paperInfo.paperInfoTable.add(paperInfo.obligationCoupon);
                        paperInfo.paperInfoTable.add(paperInfo.NKD);
                        paperInfo.paperInfoTable.add(paperInfo.creditRating);
                        paperInfo.paperInfoTable.add(paperInfo.nextCouponDate);
                        paperInfo.paperInfoTable.add(paperInfo.economicsSector);
                        paperInfo.paperInfoTable.add(paperInfo.closePrice);
                        paperInfo.paperInfoTable.add(paperInfo.openPrice);
                        paperInfo.paperInfoTable.add(paperInfo.actualPrice);
                        paperInfo.paperInfoTable.add(paperInfo.priceChange);

                        receivedItem = e.getItem().toString();
                        Element hrefElement = obligationInfoSite_01.select(STR."a:containsOwn(\{receivedItem})").first();
                        assert hrefElement != null;
                        String href = hrefElement.attr("href");
                        try {
                            Document tempHref = Jsoup.connect(STR."https://mfd.ru\{href}").userAgent("Mozilla/5.0 " +
                                    "(Windows NT 10.0; Win64; x64) " +
                                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                    "Chrome/58.0.3029.110 Safari/537.36").get();
                            Element obligationCode = tempHref.getElementsByTag("h1").first();
                            assert obligationCode != null;
                            obligationISIN = STR."RU00\{obligationCode.text().split("RU00|:")[1].trim()}";
                            paperInfo.paperISIN.setText(STR."ISIN: \{obligationISIN}");
                            Elements tds = tempHref.select("tr td");
                            for (Element td : tds) {
                                if (td.text().equals("Пред закр")) {
                                    paperInfo.closePrice.setText(STR."Закрытие: \{Objects.requireNonNull(td.nextElementSibling()).text()}%");
                                }
                                if (td.text().equals("Открытие")) {
                                    paperInfo.openPrice.setText(STR."Открытие: \{Objects.requireNonNull(td.nextElementSibling()).text()}%");
                                }
                            }

                            driver.get(STR."https://smart-lab.ru/q/bonds/\{obligationISIN}");
                            Document obligationDataSite = Jsoup.parse(driver.getPageSource());
//                            Document obligationDataSite = Jsoup.connect(STR."https://smart-lab.ru/q/bonds/\{obligationISIN}").userAgent("Mozilla/5.0 " +
//                                    "(Windows NT 10.0; Win64; x64) " +
//                                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
//                                    "Chrome/58.0.3029.110 Safari/537.36").get();

                            paperInfo.NKD.setText(STR."НКД: \{obligationDataSite.select("div[title='Накопленный купонный доход']")
                                    .select("div").get(2).text()}");

                            Elements divs = obligationDataSite.select("div.quotes-simple-table__item");
                            String currency = " ";
                            for (Element div : divs) {
                                if (div.text().equals("Котировка облигации, %")) {
                                    paperInfo.actualPrice.setText(STR."Цена: \{Objects.requireNonNull(div.nextElementSibling()).text()}");
                                }
                                if (div.text().equals("Изм за день, %")) {
                                    paperInfo.priceChange.setText(STR."Изм. за день: \{Objects.requireNonNull(div.nextElementSibling()).text()}");
                                }
                                if (div.text().equals("Валюта")) {
                                    currency = Objects.requireNonNull(div.nextElementSibling()).text();
                                }
                                if (div.text().equals("Номинал")) {
                                    paperInfo.obligationNominal.setText(STR."Номинал: \{Objects.requireNonNull(div.nextElementSibling()).text()}");
                                }
                                if (div.text().equals("Ставка купона")) {
                                    paperInfo.obligationCoupon.setText(STR."Купон: \{Objects.requireNonNull(div.nextElementSibling()).text()}");
                                }
                                if (div.text().equals("Дата след. выплаты")) {
                                    paperInfo.nextCouponDate.setText(STR."След. выплата: \{Objects.requireNonNull(div.nextElementSibling()).text()}");
                                }
                                if (div.text().equals("Сектор")) {
                                    paperInfo.economicsSector.setText(STR."Сектор: \{Objects.requireNonNull(div.nextElementSibling()).text()}");
                                }
                            }
                            paperInfo.creditRating.setText(STR."Кред. рейтинг: \{obligationDataSite.getElementsByClass("linear-progress-bar__text").text()}");
                            if (paperInfo.creditRating.getText().equals("Кред. рейтинг: ")) paperInfo.creditRating.setText("В процессе присвоения");
                            if (paperInfo.economicsSector.getText().equals("Государственные")) paperInfo.creditRating.setText("Государственная облигация");
                            if (paperInfo.economicsSector.getText().equals("Субфедеральные")) paperInfo.creditRating.setText("Субфед. облигация");
                            if (currency.equals("SUR")) {
                                paperInfo.obligationNominal.setText(STR."\{paperInfo.obligationNominal.getText()} RUB");
                            } else {
                                paperInfo.obligationNominal.setText(STR."\{paperInfo.obligationNominal.getText()} \{currency}");
                            }
                            paperInfo.paperName.setText(receivedItem);
                            paperInfo.paperType.setText("Облигация");

                            paperInfo.addPaper.setEnabled(true);
                            for (Component component : papersList.getComponents()) {
                                if (component instanceof userPortfolioPaper button) {
                                    if (button.getText().equals(e.getItem().toString())) {
                                        paperInfo.addPaper.setEnabled(false);
                                        break;
                                    }
                                }
                            }
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                        System.out.println(STR."\{receivedItem} \{obligationISIN}");
                    }
                });
            });

            papersList.addContainerListener(new ContainerListener() {
                @Override
                public void componentAdded(ContainerEvent e) {
                    if (e.getChild() instanceof userPortfolioPaper button) {
                        processor.addOrUpdate(button.economicSector, userPortfolioPaper.sumPrice);

                        portfolioData.circleGraphicEconomicSectors.updateData(processor.getData());
                        System.out.println(Arrays.deepToString(processor.getData()));

                        paperInfo.finalSum += userPortfolioPaper.sumPrice;
                        paperInfo.getSum(paperInfo.finalSum);
                        analyticsData.onlineExchangeData.updateUI();
                        if (button.paperType.equals("Акция")) {
                            portfolioData.valuesForCircle[0] += userPortfolioPaper.sumPrice;
                        } else {
                            portfolioData.valuesForCircle[1] += userPortfolioPaper.sumPrice;
                        }
                    }
                    portfolioButtons.portfolioCost.updateUI();
                    portfolioData.circleGraphicTypesOfPapers.revalidate();
                    portfolioData.circleGraphicTypesOfPapers.repaint();
                    portfolioData.circleGraphicEconomicSectors.revalidate();
                    portfolioData.circleGraphicEconomicSectors.repaint();
                }

                @Override
                public void componentRemoved(ContainerEvent e) {
                }
            });

            papersButtons.setLayout(new GridLayout(2, 1));
            JPanel buttons = new JPanel();
            buttons.setLayout(new GridLayout(1, 2));
            buttons.add(action);
            buttons.add(obligation);
            papersButtons.add(buttons);
            papersButtons.add(comboBox);

            add(papersButtons, BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);
        }
    }
    public static class portfolioButtons extends JPanel { //кнопки в блоке "портфель"
        papers papers;
        static JLabel portfolioCost = new JLabel("Стоимость") {
            {
                setFont(mainFont);
                setBorder(new LineBorder(Color.black));
                setHorizontalAlignment(SwingConstants.CENTER);
            }
        };
        userButton addBrokerReport = new userButton("<html><div style='text-align: center;'>Внести через<br>отчёт брокера</div></html>");
        userButton renewPanel = new userButton("Сбросить всё");
        ArrayList<String> isinsFromBroker = new ArrayList<>();
        ArrayList<String> namesFromBroker = new ArrayList<>();
        ArrayList<Integer> countsFromBroker = new ArrayList<>();
        static String[] isinsForSearching;
        static String[] namesForSearching;
        static Integer[] countsOfPapers;
        public portfolioButtons(papers searchedPapers) {
            this.papers = searchedPapers;
            JFileChooser brokerReportChooser = new JFileChooser();
            addBrokerReport.addActionListener(_ -> {
                brokerReportChooser.setDialogTitle("Выберите HTML-файл отчёта брокера");
                int userSelection = brokerReportChooser.showOpenDialog(null);
                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = brokerReportChooser.getSelectedFile();
                    try {
                        Document doc = Jsoup.parse(selectedFile, "UTF-8");
                        Element searchBorder = doc.select("p:containsOwn(Денежные средства)").first();
                        assert searchBorder != null;
                        Element previousSibling = searchBorder.previousElementSibling();
                        assert previousSibling != null;
                        for (Element isin : previousSibling) {
                            if (isin.text().startsWith("RU0")) {
                                isinsFromBroker.add(isin.text());
                                namesFromBroker.add(Objects.requireNonNull(isin.previousSibling()).toString().split(">|</")[1]);
                                countsFromBroker.add(Integer.valueOf(isin.nextElementSiblings().get(6).text()));
                            }
                        }
                        isinsForSearching = isinsFromBroker.toArray(String[]::new);
                        namesForSearching = namesFromBroker.toArray(String[]::new);
                        countsOfPapers = countsFromBroker.toArray(Integer[]::new);

                        String actualPrice = " ";
                        String priceChange = " ";
                        double nominal = 1000.0;

                        for (int i = 0; i < isinsForSearching.length; i++) {
                            driver.get(STR."https://smart-lab.ru/q/bonds/\{isinsForSearching[i]}");
                            Document obligationDataSite = Jsoup.parse(driver.getPageSource());

                            Elements divs = obligationDataSite.select("div.quotes-simple-table__item");
                            String currency = " ";
                            String sector = " ";
                            for (Element div : divs) {
                                if (div.text().equals("Котировка облигации, %")) {
                                    actualPrice = Objects.requireNonNull(div.nextElementSibling()).text().replace("%", "");
                                }
                                if (div.text().equals("Изм за день, %")) {
                                    priceChange = Objects.requireNonNull(div.nextElementSibling()).text();
                                }
                                if (div.text().equals("Номинал")) {
                                    nominal = Double.parseDouble(Objects.requireNonNull(div.nextElementSibling()).text());
                                }
                                if (div.text().equals("Валюта")) {
                                    currency = Objects.requireNonNull(div.nextElementSibling()).text();
                                }
                                if (div.text().equals("Сектор")) {
                                    sector = Objects.requireNonNull(div.nextElementSibling()).text();
                                }
                            }
                            switch (currency) {
                                case "USD":
                                    nominal *= analyticsData.usdCourse;
                                    break;
                                case "EUR":
                                    nominal *= analyticsData.eurCourse;
                                    break;
                                case "CNY":
                                    nominal *= analyticsData.cnyCourse;
                                    break;
                            }
                            papers.papersList.add(new userPortfolioPaper(STR."<html><table><tr><td rowspan=\"2\" style='text-align: left; margin-right: 10'>\{namesForSearching[i]}</td>" +
                                    STR."<td style='text-align: right;'>\{actualPrice}</td><td rowspan=\"2\" style='text-align: right;'>\{countsOfPapers[i]} шт.</td></tr><tr><td style='text-align: right;'>\{priceChange}</td></tr>",
                                    namesForSearching[i], "Облигация", actualPrice, nominal, countsOfPapers[i], sector));
                            papers.papersList.add(Box.createRigidArea(new Dimension(3, 10)));
                            papers.papersList.updateUI();
                            portfolioData.circleGraphicTypesOfPapers.revalidate();
                            portfolioData.circleGraphicTypesOfPapers.repaint();
                        }
                    } catch (IOException e) {
                        System.err.println("ОШИБКА");
                    }
                }
//                System.out.println(paperInfo.finalSum);
            });
            setSize(210, 270);
            setBackground(Color.white);
            setBorder(new CompoundBorder(new LineBorder(Color.BLACK), new EmptyBorder(10, 10, 10, 10)));
            setLayout(new GridLayout(3, 1, 5, 10));
            add(portfolioCost);
            add(addBrokerReport);
            add(renewPanel);
        }
    }
    public static class paperInfo extends defaultPanel { //таблица с данными по бумаге
        papers papers;
        static userButton addPaper = new userButton("Добавить");
        static userButton removeButton = new userButton("Удалить");
        static defaultLabel paperName = new defaultLabel(" ");
        static defaultLabel paperISIN = new defaultLabel("ISIN");
        static defaultLabel paperType = new defaultLabel(" ");
        static defaultLabel actualPrice = new defaultLabel(" ");
        static defaultLabel openPrice = new defaultLabel(" ");
        static defaultLabel closePrice = new defaultLabel(" ");
        static defaultLabel priceChange = new defaultLabel(" ");
        static defaultLabel obligationCoupon = new defaultLabel(" ");
        static defaultLabel obligationNominal = new defaultLabel(" ");
        static defaultLabel NKD = new defaultLabel(" ");
        static defaultLabel creditRating = new defaultLabel(" ");
        static defaultLabel nextCouponDate = new defaultLabel(" ");
        static defaultLabel economicsSector = new defaultLabel(" ");
        static defaultLabel emptyTableFiller = new defaultLabel("<html>Выберите бумагу в выпадающем списке,<br>чтобы отобразить данные по ней</html>");
        static JPanel paperInfoTable = new JPanel();
        static double finalSum = 0;
        public static void getSum(double sum) {
            portfolioButtons.portfolioCost.setText(STR."<html>Ст-ть портфеля:<br> \{String.format("%.3f", sum)} руб.</html>");
            analyticsData.setSum(sum);
        }
        Object[] textForPortfolioButtons = new Object[7];
        private final List<ButtonClickListener> listeners = new ArrayList<>();
        public interface ButtonClickListener {
            void onButtonClicked(Object[] data);
        }
        public void addButtonClickListener(ButtonClickListener listener) {
            listeners.add(listener);
        }
        public paperInfo(papers sharedPapers) {
            this.papers = sharedPapers;
            setLayout(new BorderLayout());
            setSize(730, 370);

            JPanel paperInfoButtons = new JPanel();
            paperInfoButtons.setBorder(new EmptyBorder(5, 5, 5, 5));
            paperInfoButtons.setLayout(new GridLayout(1, 3, 5, 5));

            NumberFormat format = NumberFormat.getIntegerInstance();
            format.setGroupingUsed(false);
            JFormattedTextField numberOfPapers = new JFormattedTextField(format);
            numberOfPapers.setText("1");
            numberOfPapers.setFont(mainFont);

            removeButton.setEnabled(false);
            addPaper.setMargin(new Insets(5, 10, 5, 10));
            addPaper.setEnabled(false);
            removeButton.setMargin(addPaper.getMargin());
            paperInfoButtons.add(numberOfPapers);
            paperInfoButtons.add(addPaper);
            paperInfoButtons.add(removeButton);

            addPaper.addActionListener(_ -> {
                int countOfPapers = Integer.parseInt(numberOfPapers.getText());

                textForPortfolioButtons[2] = paperType.getText();
                textForPortfolioButtons[5] = countOfPapers;


                if (paperType.getText().equals("Акция")) {
                    textForPortfolioButtons[0] = STR."<html><table><tr><td rowspan=\"2\" style='text-align: left;'>\{paperName.getText()}</td>" +
                            STR."<td style='text-align: right;'>\{actualPrice.getText().split(" ", 2)[1]}" +
                            STR."</td><td rowspan=\"2\" style='text-align: right;'>\{countOfPapers} шт.</td></tr><tr><td style='text-align: right;'>\{priceChange.getText().split(" ", 4)[3]}</td></tr>";
                    textForPortfolioButtons[1] = paperName.getText().split(" ", 2)[1];
                    textForPortfolioButtons[3] = actualPrice.getText().split(" ", 2)[1];
                    textForPortfolioButtons[4] = 1.0;
                    textForPortfolioButtons[6] = paperISIN.getText().split(" ", 2)[1];
                } else {
                    textForPortfolioButtons[0] = STR."<html><table><tr><td rowspan=\"2\" style='text-align: left; margin-right: 10'>\{paperName.getText()}</td>" +
                            STR."<td style='text-align: right;'>\{actualPrice.getText().split(" ", 2)[1]}"+
                            STR."</td><td rowspan=\"2\" style='text-align: right;'>\{countOfPapers} шт.</td></tr><tr><td style='text-align: right;'>\{priceChange.getText().split(" ", 4)[3]}</td></tr>";
                    textForPortfolioButtons[1] = paperName.getText();
                    textForPortfolioButtons[3] = actualPrice.getText().split(" ", 2)[1].replace("%", "");
                    if (obligationNominal.getText().split(" ", 3)[2].equals("RUB")) {
                        textForPortfolioButtons[4] = Double.parseDouble(obligationNominal.getText().split(" ", 3)[1]);
                    } else {
                        switch (obligationNominal.getText().split(" ", 3)[2]) {
                            case "USD":
                                textForPortfolioButtons[4] = Integer.parseInt(obligationNominal.getText().split(" ", 3)[1]) * analyticsData.usdCourse;
                                break;
                            case "EUR":
                                textForPortfolioButtons[4] = Integer.parseInt(obligationNominal.getText().split(" ", 3)[1]) * analyticsData.eurCourse;
                                break;
                            case "CNY":
                                textForPortfolioButtons[4] = Integer.parseInt(obligationNominal.getText().split(" ", 3)[1]) * analyticsData.cnyCourse;
                                break;
                        }
                    }
                    textForPortfolioButtons[6] = economicsSector.getText().split(" ", 2)[1];
                }
                portfolioData.circleGraphicTypesOfPapers.revalidate();
                portfolioData.circleGraphicTypesOfPapers.repaint();
                portfolioData.circleGraphicEconomicSectors.revalidate();
                portfolioData.circleGraphicEconomicSectors.repaint();


                for (ButtonClickListener listener : listeners) {
                    listener.onButtonClicked(textForPortfolioButtons);
                }

                portfolioButtons.portfolioCost.setText(STR."<html>Ст-ть портфеля:<br> \{String.format("%.3f", finalSum)} руб.</html>");;
                //analyticsData.onlineExchangeData.add(new defaultLabel(portfolioButtons.portfolioCost.getText().replace("<br>", "")));
                portfolioButtons.portfolioCost.updateUI();
                analyticsData.onlineExchangeData.updateUI();
                addPaper.setEnabled(false);

            });

            removeButton.addActionListener(_ -> {
                for (Component component : papers.papersList.getComponents()) {
                    if (component instanceof userPortfolioPaper button && button.getBackground() == Color.yellow) {
                        papers.papersList.remove(button);
                        papers.removePaper(button.getText());
                        addPaper.setEnabled(false);
                        removeButton.setEnabled(false);
                        papers.papersList.updateUI();
                        analyticsData.papersList.updateUI();
                        break;
                    }
                }
            });

            addButtonClickListener(papers);
            paperInfoTable.setLayout(new GridLayout(1, 1));
            paperInfoTable.add(emptyTableFiller);
            add(paperInfoTable, BorderLayout.CENTER);
            add(paperName, BorderLayout.NORTH);
            add(paperInfoButtons, BorderLayout.SOUTH);
        }
    }
    public static class portfolioData extends JPanel { //блок данных "портфель"
        paperInfo paperInfo;
        static Object[][] valuesForSectors = {{"Сектор 1", 0.0}, {"Сектор 2", 0.0}};
        static double[] valuesForCircle = new double[]{0, 0};
        String[] labelsForCircle = new String[]{"Акции", "Облигации"};
        Color[] colorsForCircle = new Color[]{Color.green, Color.red};
        static PieChartPanelTypes circleGraphicTypesOfPapers;
        static PieChartPanelSectors circleGraphicEconomicSectors;
        portfolioButtons portfolioButtons;
        JTabbedPane tabbedPane = new JTabbedPane();
        public portfolioData(papers sharedPapers) {
            this.paperInfo = new paperInfo(sharedPapers);
            portfolioButtons = new portfolioButtons(sharedPapers);
            portfolioButtons.setLocation(405, 400);
            setLayout(null);
            setSize(1145, 681);
            setLocation(120, 0);
            setBackground(mainColor1);
            setBorder(null);
            sharedPapers.setLocation(10, 10);
            paperInfo.setLocation(405, 10);
            tabbedPane.setSize(500, 270);
            tabbedPane.setLocation(635, 400);
            circleGraphicTypesOfPapers = new PieChartPanelTypes(valuesForCircle, labelsForCircle, colorsForCircle);
            circleGraphicEconomicSectors = new PieChartPanelSectors(valuesForSectors);
            tabbedPane.addTab("Тип актива", circleGraphicTypesOfPapers);
            tabbedPane.addTab("Сектор экономики", circleGraphicEconomicSectors);
            add(sharedPapers);
            add(paperInfo);
            add(portfolioButtons);
            add(tabbedPane);
        }
    }
    public static class analyticsData extends JPanel { //блок данных "аналитика рынка"
        static double usdCourse;
        static double eurCourse;
        static double cnyCourse;
        static defaultPanel onlineExchangeData = new defaultPanel() {
            {
                setLayout(new GridLayout(7, 1));
                setLocation(405, 10);
                setSize(730, 250);
            }
        };
        userButton[] timeButtons = new userButton[4];
        int[] daysForGraphic = {10, 30, 60, 0};
        int days = 10;
        JPanel timeButtonsPanel = new JPanel(new GridLayout(1, 4, 0, 0)) {
            {
                timeButtons[0] = new userButton("10 дней");
                timeButtons[1] = new userButton("30 дней");
                timeButtons[2] = new userButton("60 дней");
                timeButtons[3] = new userButton("С начала года");
                for (int i = 0; i < 4; i++) {
                    add(timeButtons[i]);
                    int finalI = i;
                    timeButtons[i].addActionListener(_ -> {
                        days = daysForGraphic[finalI];
                        try {
                            for (Component component : priceGraphic.getComponents()) {
                                if (component instanceof GraphPanel) {
                                    priceGraphic.remove(component);
                                }
                            }
                            graphPanel = new GraphPanel(papersOperations.parseAndWritePrices(ticker),
                                    papersOperations.parseAndWriteDate(ticker), days);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        priceGraphic.add(graphPanel, BorderLayout.CENTER);
                        graphPanel.updateUI();
                    });
                }
            }
        };
        String ticker = "";
        defaultPanel priceGraphic = new defaultPanel() {
            {
                setLayout(new BorderLayout());
                add(timeButtonsPanel, BorderLayout.SOUTH);
                setLocation(405, 280);
                setSize(730, 381);
            }
        };
        static JPanel papersList;
        GraphPanel graphPanel;
        private static String textFromButtons = "Стоимость портфеля";
        public static void setSum(double sum) {
            textFromButtons = STR."Стоимость портфеля: \{sum} руб.";
        }
        public analyticsData(papers sharedPapers) throws IOException {
            papersList = sharedPapers.getPapersList_2();
            Document doc = Jsoup.connect("https://cbr.ru/currency_base/daily/").userAgent("Mozilla/5.0 " +
                    "(Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/58.0.3029.110 Safari/537.36").get();
            usdCourse = Double.parseDouble(Objects.requireNonNull(Objects.requireNonNull(doc.select("td:containsOwn(Доллар США)").first()).nextElementSibling()).text().replace(",", "."));
            eurCourse = Double.parseDouble(Objects.requireNonNull(Objects.requireNonNull(doc.select("td:containsOwn(Евро)").first()).nextElementSibling()).text().replace(",", "."));
            cnyCourse = Double.parseDouble(Objects.requireNonNull(Objects.requireNonNull(doc.select("td:containsOwn(Юань)").first()).nextElementSibling()).text().replace(",", "."));
            onlineExchangeData.add(new defaultLabel(STR."Валюты (ЦБ): USD \{usdCourse} | EUR \{eurCourse} | CNY \{cnyCourse}"));
            doc = Jsoup.connect("https://ru.investing.com/indices/mcx").userAgent("Mozilla/5.0 " +
                    "(Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/58.0.3029.110 Safari/537.36").get();
            onlineExchangeData.add(new defaultLabel(STR."IMOEX: \{doc
                    .getElementsByClass("text-5xl/9 font-bold text-[#232526] md:text-[42px] md:leading-[60px]").text()}"));
            doc = Jsoup.connect("https://ru.investing.com/indices/rtsi").userAgent("Mozilla/5.0 " +
                    "(Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/58.0.3029.110 Safari/537.36").get();
            onlineExchangeData.add(new defaultLabel(STR."RTS: \{doc
                    .getElementsByClass("text-5xl/9 font-bold text-[#232526] md:text-[42px] md:leading-[60px]").text()}"));
            doc = Jsoup.connect("https://ru.investing.com/commodities/gold").userAgent("Mozilla/5.0 " +
                    "(Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/58.0.3029.110 Safari/537.36").get();
            onlineExchangeData.add(new defaultLabel(STR."Золото (биржа): \{doc
                    .getElementsByClass("text-5xl/9 font-bold text-[#232526] md:text-[42px] md:leading-[60px]").text()}"));
//            driver.get("https://cbr.ru/hd_base/ruonia/");
//            doc = Jsoup.parse(driver.getPageSource());
//            onlineExchangeData.add(new defaultLabel(STR."RUONIA: \{Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(doc.select("td:containsOwn(Ставка RUONIA, %)")
//                    .first()).nextElementSibling()).nextElementSibling()).text()}"));

            onlineExchangeData.add(new defaultLabel("Ставка ЦБ: 20%"));
            //onlineExchangeData.add(portfolioButtons.portfolioCost);
            onlineExchangeData.add(new defaultLabel(textFromButtons)); //TODO

            papersList.addContainerListener(new ContainerListener() {
                @Override
                public void componentAdded(ContainerEvent e) {
                    if (e.getChild() instanceof userPortfolioPaper button) {
                        button.addActionListener(_ -> {
                            ticker = Jsoup.parse(button.getText()).select("[style*='text-align: left']")
                                    .text().split(" ", 2)[0].replace("#", "").toLowerCase();
                            try {
                                for (Component component : priceGraphic.getComponents()) {
                                    if (component instanceof GraphPanel) {
                                        priceGraphic.remove(component);
                                    }
                                }
                                graphPanel = new GraphPanel(papersOperations.parseAndWritePrices(ticker),
                                        papersOperations.parseAndWriteDate(ticker), days);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                            priceGraphic.add(graphPanel, BorderLayout.CENTER);
                        });
                    }
                }

                @Override
                public void componentRemoved(ContainerEvent e) {}
            });

            setLayout(null);
            setBorder(null);
            setVisible(false);

            setSize(1145, 681);
            setLocation(120, 0);
            setBackground(mainColor1);

            JScrollPane scrollPane = new JScrollPane(papersList);
            scrollPane.setBounds(10, 10, 375, 660);
            scrollPane.revalidate();
            scrollPane.repaint();

            add(scrollPane);
            add(onlineExchangeData);
            add(priceGraphic);
        }
    }
    public static class tradingStrategies extends JPanel { //панель блока "торговые стратегии
        defaultPanel buying = new defaultPanel() {
            {
                setLayout(new BorderLayout());
            }
        };
        defaultPanel selling = new defaultPanel() {
            {
                setLayout(new BorderLayout());
            }
        };
        defaultPanel holding = new defaultPanel() {
            {
                setLayout(new BorderLayout());
            }
        };
        papers buyingPapers = new papers();
        papers sellingPapers = new papers();
        papers holdingPapers = new papers();
        userButton buyigButton = new userButton("Уведомления");
        userButton sellingButton = new userButton("Уведомления");
        userButton holdingButton = new userButton("Уведомления");
        public tradingStrategies() {
            setSize(1145, 681);
            setLocation(120, 0);
            setBackground(mainColor1);
            setBorder(new EmptyBorder(10, 10, 10, 10));
            setVisible(false);
            setLayout(new GridLayout(1, 3, 20, 10));

            buying.add(new defaultLabel("Покупка"), BorderLayout.NORTH);
            selling.add(new defaultLabel("Продажа"), BorderLayout.NORTH);
            holding.add(new defaultLabel("Держать"), BorderLayout.NORTH);

            buying.add(buyingPapers, BorderLayout.CENTER);
            selling.add(sellingPapers, BorderLayout.CENTER);
            holding.add(holdingPapers, BorderLayout.CENTER);

            buying.add(buyigButton, BorderLayout.SOUTH);
            selling.add(sellingButton, BorderLayout.SOUTH);
            holding.add(holdingButton, BorderLayout.SOUTH);
            add(buying);
            add(selling);
            add(holding);
        }
    }
    public static class papersOperations extends JPanel { //панель блока "операции с бумагами"
        defaultPanel correlation = new defaultPanel() {
            {
                setSize(580, 420);
                setLocation(555, 10);
                setLayout(null);
            }
        };
        userButton addToCorrelation = new userButton("Добавить >>") {
            {
                setSize(130, 40);
                setLocation(405, 160);

            }
        };
        userButton deleteFromCorrelation = new userButton("<< Убрать") {
            {
                setSize(130, 40);
                setLocation(405, 220);
            }
        };
        private GridLayout gridLayoutForRebalance;
        private int rowsForRebalance = 1;
        defaultPanel panelForRebalance = new defaultPanel() {
            {
                setSize(580, 221);
                setLocation(555, 450);
                setLayout(new BorderLayout());
            }
        };
        defaultPanel rebalance = new defaultPanel();
        static JTextField daysForRebalance = new JTextField("Введите количество дней для ребалансировки") {
            {
                ((AbstractDocument) getDocument()).setDocumentFilter(rebalanceButton.numericFilter);
                addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        setText("14");
                    }
                });
                getDocument().addDocumentListener(new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        updateRebalance.setEnabled(!(getText().trim().isEmpty() || getText().equals("0")));
                    }
                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        updateRebalance.setEnabled(!(getText().trim().isEmpty() || getText().equals("0")));
                    }
                    @Override
                    public void changedUpdate(DocumentEvent e) {
                    }
                });
                setFont(mainFont);
            }
        };
        userButton addToRebalance = new userButton("Добавить >>") {
            {
                setSize(130, 40);
                setLocation(405, 510);
            }
        };
        userButton deleteFromRebalance = new userButton("<< Убрать") {
            {
                setSize(130, 40);
                setLocation(405, 570);
            }
        };
        userButton updateCorrelationPanel = new userButton("<html><div style='text-align: center;'>Обновить таблицу для:<br></div></html>") {
            {
                setSize(150,150);
                setLocation(215, 135);
                setBackground(Color.lightGray);
            }
        };
        public static double calculateCorrelation(double[] x, double[] y) {
            if (x == null || y == null || x.length != y.length) {
                throw new IllegalArgumentException("Массивы должны быть одинаковой длины и не null");
            }

            int n = x.length;
            double sumX = 0.0, sumY = 0.0, sumXY = 0.0;
            double sumXSquare = 0.0, sumYSquare = 0.0;

            // Вычисляем средние значения
            for (int i = 0; i < n; i++) {
                sumX += x[i];
                sumY += y[i];
            }
            double meanX = sumX / n;
            double meanY = sumY / n;

            // Вычисляем суммы для числителя и знаменателя
            for (int i = 0; i < n; i++) {
                double xDiff = x[i] - meanX;
                double yDiff = y[i] - meanY;
                sumXY += xDiff * yDiff;
                sumXSquare += xDiff * xDiff;
                sumYSquare += yDiff * yDiff;
            }

            // Вычисляем корреляцию
            if (sumXSquare == 0 || sumYSquare == 0) {
                return 0.0; // Если одно из отклонений нулевое, корреляция не определена (возвращаем 0)
            }
            return sumXY / (Math.sqrt(sumXSquare) * Math.sqrt(sumYSquare));
        }
        public static double[] calculatePercentageReturns(double[] prices) {
            if (prices == null || prices.length < 2) {
                throw new IllegalArgumentException("Массив цен должен содержать как минимум 2 значения");
            }

            double[] returns = new double[prices.length - 1];
            for (int i = 1; i < prices.length; i++) {
                returns[i - 1] = (prices[i] - prices[i - 1]) / prices[i - 1];
            }
            return returns;
        }
        public static String[] parseAndWriteDate(String lowTicker) throws IOException {
            ArrayList<String> datesByTicker = new ArrayList<>();

            Document doc1 = Jsoup.connect(STR."https://swingtrading.ru/moex/stock/\{lowTicker}/history/").userAgent(
                    "Mozilla/5.0 " +
                            "(Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/58.0.3029.110 Safari/537.36").get();
            Elements elements = doc1.select("tr[title]");
            DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern("dd.MM");
            for (Element tr : elements) {
                datesByTicker.add((LocalDate.parse(tr.select("td").getFirst().text(), inputFormat)).format(outputFormat));
            }
            String[] dates = new String[datesByTicker.size()];
            for (int i = 0; i < datesByTicker.size(); i++) {
                dates[i] = datesByTicker.get(i);
            }
            return dates;
        }
        public static double[] parseAndWritePrices(String lowTicker) throws IOException {
            ArrayList<Double> pricesByDays = new ArrayList<>();

            Document doc1 = Jsoup.connect(STR."https://swingtrading.ru/moex/stock/\{lowTicker}/history/").userAgent(
                    "Mozilla/5.0 " +
                    "(Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/58.0.3029.110 Safari/537.36").get();
            Elements elements = doc1.select("tr[title]");
            for (Element tr : elements) {
                pricesByDays.add(Double.valueOf(Objects.requireNonNull(Objects.requireNonNull(tr.select("td").
                        getFirst().nextElementSibling()).nextElementSibling()).text()));
            }
            double[] data = new double[pricesByDays.size()];
            for (int i = 0; i < pricesByDays.size(); i++) {
                data[i] = pricesByDays.get(i);
            }
            return data;
        }
        public static String[][] rebalancePortfolio(double[][] closingPrices, double[] targetWeights, double[] currentWeights, int[] counts) { //расчет ребалансировки
            int numAssets = closingPrices.length;
            int days = closingPrices[0].length;

            // 1. Рассчитываем дневные доходности для каждого актива
            double[][] returns = new double[numAssets][days - 1];
            for (int i = 0; i < numAssets; i++) {
                for (int j = 1; j < days; j++) {
                    returns[i][j - 1] = (closingPrices[i][j] - closingPrices[i][j - 1]) / closingPrices[i][j - 1];
                }
            }

            // 2. Рассчитываем текущую стоимость каждого актива (последняя цена)
            double[] currentValues = new double[numAssets];
            for (int i = 0; i < numAssets; i++) {
                currentValues[i] = closingPrices[i][days - 1];
            }

            // 3. Рассчитываем стоимость портфеля
            double totalPortfolioValue = 0;
            for (int i = 0; i < numAssets; i++) {
                totalPortfolioValue += closingPrices[i][days - 1] * counts[i];
            }

            // 4. Определяем необходимые изменения для ребалансировки
//            System.out.println(STR."Текущие веса: \{Arrays.toString(currentWeights)}");
//            System.out.println(STR."Целевые веса: \{Arrays.toString(targetWeights)}");

            double[] adjustments = new double[numAssets];
            for (int i = 0; i < numAssets; i++) {
                adjustments[i] = targetWeights[i] - currentWeights[i];
            }

            // 5. Рассчитываем сумму для покупки/продажи каждого актива
            double[] adjustmentAmounts = new double[numAssets];
            for (int i = 0; i < numAssets; i++) {
                adjustmentAmounts[i] = adjustments[i] * totalPortfolioValue;
            }

            // Выводим результаты
//            System.out.println("\nРебалансировка портфеля:");
            String[][] returnableSum = new String[numAssets][3];
            for (int i = 0; i < numAssets; i++) {
                if (adjustmentAmounts[i] > 0) {
                    returnableSum[i][0] = STR."\{String.valueOf(String.format("%.2f", adjustmentAmounts[i]))} руб.";
                    returnableSum[i][1] = "Покупать";
                    returnableSum[i][2] = String.valueOf(String.format("%.0f", adjustmentAmounts[i] / closingPrices[i][days - 1]));
                    //returnableSum[i][2] = String.valueOf((BigDecimal.valueOf(adjustmentAmounts[i] / closingPrices[i][days - 1])).setScale(0, RoundingMode.HALF_EVEN).doubleValue());
                } else if (adjustmentAmounts[i] < 0) {
                    returnableSum[i][0] = STR."\{String.valueOf(String.format("%.2f", -adjustmentAmounts[i]))} руб.";
                    returnableSum[i][1] = "Продавать";
                    returnableSum[i][2] = String.valueOf(String.format("%.0f", -adjustmentAmounts[i] / closingPrices[i][days - 1]));
                    //returnableSum[i][2] = String.valueOf((BigDecimal.valueOf(-adjustmentAmounts[i] / closingPrices[i][days - 1])).setScale(0, RoundingMode.HALF_EVEN).doubleValue());
                } else {
                    returnableSum[i][0] = "0";
                    returnableSum[i][1] = "Держать";
                    returnableSum[i][2] = "0";
                }
            }
            return (returnableSum);
            // Дополнительно: рассчитываем волатильность портфеля
//            double portfolioVolatility = calculatePortfolioVolatility(returns, targetWeights);
//            System.out.printf("\nВолатильность портфеля: %.4f\n", portfolioVolatility);
//            return totalPortfolioValue;
        }
        // Метод для расчета волатильности портфеля
        public static double calculatePortfolioVolatility(double[][] returns, double[] weights) {
            int numAssets = returns.length;
            int numDays = returns[0].length;

            // Рассчитываем ковариационную матрицу
            double[][] covarianceMatrix = new double[numAssets][numAssets];

            for (int i = 0; i < numAssets; i++) {
                for (int j = 0; j < numAssets; j++) {
                    double sum = 0;
                    double meanI = Arrays.stream(returns[i]).average().orElse(0);
                    double meanJ = Arrays.stream(returns[j]).average().orElse(0);

                    for (int k = 0; k < numDays; k++) {
                        sum += (returns[i][k] - meanI) * (returns[j][k] - meanJ);
                    }

                    covarianceMatrix[i][j] = sum / (numDays - 1);
                }
            }

            // Рассчитываем волатильность портфеля
            double volatility = 0;
            for (int i = 0; i < numAssets; i++) {
                for (int j = 0; j < numAssets; j++) {
                    volatility += weights[i] * weights[j] * covarianceMatrix[i][j];
                }
            }

            return Math.sqrt(volatility);
        }
        static userButton updateRebalance = new userButton("Обновить >>") {
            {
                setSize(130, 40);
                setLocation(405, 630);
                setEnabled(false);
            }
        };
        mainFrame mainFrame;
        static defaultPanel countOfPapersInRebalance = new defaultPanel() {
            {
                setPreferredSize(new Dimension(50, 221));
            }
        };
        public static class rebalanceButton extends userButton {
            mainFrame mainFrame;
            JTextField pointWeight;
            JTextField accDelta;
            static DocumentFilter numericFilter = new DocumentFilter() {
                @Override
                public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr)
                        throws BadLocationException {
                    if (isNumeric(text)) {
                        super.insertString(fb, offset, text, attr);
                    }
                }
                @Override
                public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                        throws BadLocationException {
                    if (isNumeric(text)) {
                        super.replace(fb, offset, length, text, attrs);
                    }
                }
                private boolean isNumeric(String text) {
                    return text.matches("^\\d+$");
                }
            };
            String ticker;
            double[] data;
            double currentWeight;
            int count;
            double targetWeight;
            int approved = 0;
            public rebalanceButton(String text, String ticker, double[] data, double currentWeight, int count){
                super(text);
                this.data = data;
                this.ticker = ticker;
                this.currentWeight = Double.parseDouble(String.format(Locale.US, "%.2f", currentWeight));
                this.count = count;
                this.addActionListener(_ -> {
                    JDialog dialog = new JDialog(mainFrame, text, true);
                    dialog.setSize(400, 200);
                    dialog.setLayout(new GridLayout(3,2));
                    dialog.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {
                            setBackground(Color.gray);
                        }
                    });

                    pointWeight = new JTextField();
                    ((AbstractDocument) pointWeight.getDocument()).setDocumentFilter(numericFilter);
                    accDelta = new JTextField();
                    ((AbstractDocument) accDelta.getDocument()).setDocumentFilter(numericFilter);

                    JButton okButton = new JButton("Ввести");
                    okButton.setBackground(mainColor1);

                    MouseListener forOk = new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            okButton.setBackground(mainColor1);
                            okButton.setText("Ввести");
                        }
                    };

                    pointWeight.addMouseListener(forOk);
                    accDelta.addMouseListener(forOk);

                    okButton.addActionListener(_ -> {
                        if (pointWeight.getText().isEmpty() || accDelta.getText().isEmpty()) {
                            okButton.setBackground(Color.red);
                            okButton.setText("Заполните поля!");
                        } else {
                            dialog.dispose();
                            this.setBackground(Color.green);
                            this.setEnabled(false);
                            approved++;
                            this.targetWeight = Double.parseDouble(pointWeight.getText()) / 100;
                            countOfPapersInRebalance.add(new defaultLabel(String.valueOf(String.format("%.2f", this.targetWeight))));
                            countOfPapersInRebalance.updateUI();
                        }
                    });
                    dialog.add(new JLabel("Введите целевой вес актива, %"));
                    dialog.add(pointWeight);
                    dialog.add(new JLabel("Введите допустимые рамки, %"));
                    dialog.add(accDelta);
                    dialog.add(new JLabel(" "));
                    dialog.add(okButton);
                    dialog.setLocationRelativeTo(mainFrame);
                    dialog.setVisible(true);
                });
            }
        }
        public papersOperations(papers sharedPapers) {
            setSize(1145, 681);
            setLocation(120, 0);
            setBackground(mainColor1);
            setBorder(null);
            setVisible(false);
            setLayout(null);

            JPanel papersList = sharedPapers.getPapersList_1();
            JScrollPane scrollPane = new JScrollPane(papersList);
            scrollPane.setBounds(10, 10, 375, 660);

            addToRebalance.addActionListener(_ -> {
                for (Component component : papersList.getComponents()) {
                    if (component instanceof userPortfolioPaper button && button.getBackground() == Color.yellow) {
                        gridLayoutForRebalance = new GridLayout(rowsForRebalance, 3);
                        rebalance.setLayout(gridLayoutForRebalance);
                        countOfPapersInRebalance.setLayout(new GridLayout(rowsForRebalance, 1));
                        String paperName = Jsoup.parse(button.getText()).select("[style*='text-align: left']").text();
                        String paperPrice = Jsoup.parse(button.getText()).select("[style*='text-align: right']")
                                .getFirst().text().replace("руб.", "").trim();
                        int paperCount = Integer.parseInt(Jsoup.parse(button.getText()).select("[style*='text-align: right']").
                                get(1).text().replace("шт.", "").trim());
                        double totalPrice = Double.parseDouble(paperPrice.replace(" ", "")) * paperCount;
                        double percentageOfPaper = (totalPrice / paperInfo.finalSum) * 100;
                        String ticker = Jsoup.parse(button.getText()).select("[style*='text-align: left']")
                                .text().split(" ", 2)[0].replace("#", "");
                        rebalance.add(new rebalanceButton(paperName, ticker, new double[]{0}, percentageOfPaper / 100, paperCount));
                        rebalance.add(new defaultLabel(String.valueOf(String.format("%.3f", totalPrice))));
                        rebalance.add(new defaultLabel(String.valueOf(String.format("%.3f", percentageOfPaper))));

                        rowsForRebalance++;
                        rebalance.updateUI();
                    }
                }
            });

            updateRebalance.addActionListener(_ -> {
                int days = Integer.parseInt(daysForRebalance.getText().trim());

                int countOfButtons = 0;
                for (Component component : rebalance.getComponents()) {
                    if (component instanceof rebalanceButton button) {
                        ArrayList<Double> pricesByDays = new ArrayList<>();
                        Document doc1;
                        try {
                            doc1 = Jsoup.connect(STR."https://swingtrading.ru/moex/stock/\{button.ticker}/history/").userAgent(
                                    "Mozilla/5.0 " +
                                            "(Windows NT 10.0; Win64; x64) " +
                                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                            "Chrome/58.0.3029.110 Safari/537.36").get();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        Elements elements = doc1.select(STR."tr[title='\{Objects.requireNonNull(doc1.select("h1").first()).
                                text().split(" ", 2)[0]}']");
                        for (int i = 0; i < days; i++) {
                            pricesByDays.add(Double.valueOf(Objects.requireNonNull(Objects.requireNonNull(elements.get(i).select("td").
                                    getFirst().nextElementSibling()).nextElementSibling()).text()));
                        }
                        double[] data = new double[pricesByDays.size()];
                        for (int i = 0; i < pricesByDays.size(); i++) {
                            data[i] = pricesByDays.get(i);
                        }

                        button.data = data;
                        countOfButtons++;
                    }
                }

                String[] tickers = new String[countOfButtons];
                double[] targetWeights = new double[countOfButtons];
                double[] currentWeights = new double[countOfButtons];
                int[] counts = new int[countOfButtons];
                double[][] closingPrices = new double[countOfButtons][days];
                int i = 0;
                for (Component component : rebalance.getComponents()) {
                    if (component instanceof rebalanceButton button) {
                        targetWeights[i] = button.targetWeight;
                        currentWeights[i] = button.currentWeight;
                        counts[i] = button.count;
                        closingPrices[i] = button.data;
                        tickers[i] = button.ticker;
                        i++;
                    }
                }
                String[][] forFinalTable = rebalancePortfolio(closingPrices, targetWeights, currentWeights, counts);

                JDialog dialog = new JDialog(mainFrame, "Рекомендации", true);
                dialog.setLayout(new GridLayout(countOfButtons + 1,4));
                dialog.setSize(600, (countOfButtons + 1) * 100);
                dialog.add(new defaultLabel("Тикер"));
                dialog.add(new defaultLabel("Решение"));
                dialog.add(new defaultLabel("Кол-во ≈"));
                dialog.add(new defaultLabel("Сумма ≈"));

                for (int j = 0; j < countOfButtons; j++) {
                    dialog.add(new defaultLabel(tickers[j]));
                    dialog.add(new defaultLabel(forFinalTable[j][1]));
                    dialog.add(new defaultLabel(forFinalTable[j][2]));
                    dialog.add(new defaultLabel(forFinalTable[j][0]));
                }

                dialog.setLocationRelativeTo(mainFrame);
                dialog.setVisible(true);
            });

            int[] counterForCorr = {0};
            String[][] tableElements = new String[5][5];
            tableElements[0][0] = " ";
            String[] addedTickers = new String[4];
            correlation.add(updateCorrelationPanel);
            addToCorrelation.addActionListener(_ -> {
                for (Component component : papersList.getComponents()) {
                    if (component instanceof userPortfolioPaper button && button.getBackground() == Color.yellow) {
                        String ticker = Jsoup.parse(button.getText()).select("[style*='text-align: left']")
                                .text().split(" ", 2)[0].replace("#", "");
                        addedTickers[counterForCorr[0]] = ticker;
                        System.out.println(Arrays.toString(addedTickers));
                        counterForCorr[0]++;
                        switch (counterForCorr[0]) {
                            case 1:
                                tableElements[0][1] = addedTickers[0];
                                tableElements[1][0] = addedTickers[0];
                                tableElements[1][1] = "1";
                                updateCorrelationPanel.setText(STR."\{updateCorrelationPanel.getText()
                                        .replace("</div></html>", "")}\{addedTickers[0]}<br></div></html>");
                                break;
                            case 2:
                                tableElements[0][2] = addedTickers[1];
                                tableElements[2][0] = addedTickers[1];
                                updateCorrelationPanel.setText(STR."\{updateCorrelationPanel.getText()
                                        .replace("</div></html>", "")}\{addedTickers[1]}<br></div></html>");
                                try {
                                    tableElements[2][1] = String.valueOf(String.format("%.3f", calculateCorrelation(
                                            calculatePercentageReturns(parseAndWritePrices(addedTickers[0].toLowerCase())),
                                            calculatePercentageReturns(parseAndWritePrices(addedTickers[1].toLowerCase())))));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                tableElements[1][2] = tableElements[2][1];
                                tableElements[2][2] = "1";
                                break;
                            case 3:
                                tableElements[0][3] = addedTickers[2];
                                tableElements[3][0] = addedTickers[2];
                                updateCorrelationPanel.setText(STR."\{updateCorrelationPanel.getText()
                                        .replace("</div></html>", "")}\{addedTickers[2]}<br></div></html>");
                                try {
                                    tableElements[3][1] = String.valueOf(String.format("%.3f", calculateCorrelation(
                                            calculatePercentageReturns(parseAndWritePrices(addedTickers[0].toLowerCase())),
                                            calculatePercentageReturns(parseAndWritePrices(addedTickers[2].toLowerCase())))));
                                    tableElements[3][2] = String.valueOf(String.format("%.3f", calculateCorrelation(
                                            calculatePercentageReturns(parseAndWritePrices(addedTickers[1].toLowerCase())),
                                            calculatePercentageReturns(parseAndWritePrices(addedTickers[2].toLowerCase())))));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                tableElements[1][3] = tableElements[3][1];
                                tableElements[2][3] = tableElements[3][2];
                                tableElements[3][3] = "1";
                                break;
                            case 4:
                                tableElements[0][4] = addedTickers[3];
                                tableElements[4][0] = addedTickers[3];
                                updateCorrelationPanel.setText(STR."\{updateCorrelationPanel.getText()
                                        .replace("</div></html>", "")}\{addedTickers[3]}<br></div></html>");
                                try {
                                    tableElements[4][1] = String.valueOf(String.format("%.3f", calculateCorrelation(
                                            calculatePercentageReturns(parseAndWritePrices(addedTickers[0].toLowerCase())),
                                            calculatePercentageReturns(parseAndWritePrices(addedTickers[3].toLowerCase())))));
                                    tableElements[4][2] = String.valueOf(String.format("%.3f", calculateCorrelation(
                                            calculatePercentageReturns(parseAndWritePrices(addedTickers[1].toLowerCase())),
                                            calculatePercentageReturns(parseAndWritePrices(addedTickers[3].toLowerCase())))));
                                    tableElements[4][3] = String.valueOf(String.format("%.3f", calculateCorrelation(
                                            calculatePercentageReturns(parseAndWritePrices(addedTickers[2].toLowerCase())),
                                            calculatePercentageReturns(parseAndWritePrices(addedTickers[3].toLowerCase())))));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                tableElements[1][4] = tableElements[4][1];
                                tableElements[2][4] = tableElements[4][2];
                                tableElements[3][4] = tableElements[4][3];
                                tableElements[4][4] = "1";
                                break;
                        }
                        break;
                    }
                }
            });
            updateCorrelationPanel.addActionListener(_ -> {
                correlation.remove(updateCorrelationPanel);
                String[][] newArray = new String[counterForCorr[0]+1][counterForCorr[0]+1];
                for (int i = 0; i < newArray.length; i++) {
                    for (int j = 0; j < newArray.length; j++) {
                        newArray[i][j] = tableElements[i][j];
                    }
                }
                correlation.setLayout(new GridLayout(newArray.length, newArray.length));
                for (int i = 0; i < newArray.length; i++) {
                    for (int j = 0; j < newArray.length; j++) {
                        correlation.add(new defaultLabel(newArray[i][j]));
                    }
                }
                correlation.revalidate();
                correlation.repaint();
            });
            deleteFromCorrelation.addActionListener(_ -> {

            });

            panelForRebalance.add(rebalance, BorderLayout.CENTER);
            panelForRebalance.add(daysForRebalance, BorderLayout.NORTH);
            panelForRebalance.add(countOfPapersInRebalance, BorderLayout.WEST);
            add(updateRebalance);
            add(addToRebalance);
            add(deleteFromRebalance);
            add(panelForRebalance);
            add(deleteFromCorrelation);
            add(addToCorrelation);
            add(correlation);
            add(scrollPane);
        }
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("webdriver.chrome.driver", "C:/WebDriver/yandexdriver.exe");
        options.setBinary("C:/Users/User/AppData/Local/Yandex/YandexBrowser/Application/browser.exe");
        options.addArguments("--headless=new");
        driver = new ChromeDriver(options);
        mainFrame mainFrame = new mainFrame();
        mainFrame.setVisible(true);
    }
}