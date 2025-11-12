package org.example.store.utils;

import com.sun.javafx.print.Units;
import javafx.application.Platform;
import javafx.geometry.NodeOrientation;
import javafx.print.*;
import javafx.scene.control.TextArea;
import javafx.scene.text.*;
import javafx.scene.image.Image;

import java.io.*;
import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class InvoicePrinter {

    // convenience overload: طباعة مباشرة من CartItemDTO بدون تعديل دوال الطباعة الأصلية
    public static void printCart(int invoiceNumber, List<CartItemDTO> cartItems) {
        // نحول CartItemDTO -> PurchaseProductDTO
        List<PurchaseProductDTO> items = cartItems.stream()
                .map(ci -> new PurchaseProductDTO(
                        ci.getProductId(),
                        // افترضنا اسم getter هو getProductName()
                        ci.getProductName(),
                        ci.getUnitPrice(),
                        ci.getQuantity()
                ))
                .collect(Collectors.toList());

        double total = cartItems.stream().mapToDouble(CartItemDTO::getSubtotal).sum();
        printAndSaveInvoice(invoiceNumber, items, total, 0.0, "", "", 0.0, 0.0);
    }

    public static void printAndSaveInvoice(int invoiceNumber, List<PurchaseProductDTO> items, double totalAfterDiscount,
                                           double discountAmount, String customerName, String customerPhone,
                                           double paidAmount, double remainingAmount) {
        StringBuilder content = new StringBuilder();

        // بناء محتوى الفاتورة - كل حاجة في النص
        content.append("==========================\n");
        content.append(centerText("مطعم دهب       ", 32)).append("\n");
        content.append("==========================\n");
        content.append(centerText("فاتورة الشراء رقم      " + invoiceNumber, 32)).append("\n");
        content.append("==========================\n");

        if (!isBlank(customerName))
            content.append(centerText("العميل:        " + customerName, 32)).append("\n");
        if (!isBlank(customerPhone))
            content.append(centerText("الهاتف:        " + customerPhone, 32)).append("\n");

        LocalDate today = LocalDate.now();
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        content.append(centerText("التاريخ:       " + today, 32)).append("\n");
        content.append(centerText("الساعة:           " + time, 32)).append("\n");
        content.append("==========================\n");

        double totalBeforeDiscount = 0;

        // عرض المنتجات - في النص
        for (PurchaseProductDTO item : items) {
            if (item.getQuantity() > 0) {
                String name = item.getName().length() > 20 ?
                        item.getName().substring(0, 20) + "..." : item.getName();
                int qty = item.getQuantity();
                double subtotal = item.getSubtotal();
                totalBeforeDiscount += subtotal;

                // اسم المنتج في النص
                content.append(centerText(name, 32)).append("\n");
                // التفاصيل في النص
                String details = String.format("%d × %.2f = %.2f جنيه", qty, item.getUnitPrice(), subtotal);
                content.append(centerText(details, 32)).append("\n");
            }
        }

        double calculatedDiscountAmount = totalBeforeDiscount - totalAfterDiscount;
        double discountPercentage = totalBeforeDiscount > 0 ?
                (calculatedDiscountAmount / totalBeforeDiscount) * 100 : 0;

        content.append("==========================\n");
        content.append(centerText(String.format("الإجمالي: %.2f جنيه", totalAfterDiscount), 32)).append("\n");
        content.append("==========================\n");
        content.append(centerText("شكراً لزيارتكم       ", 32)).append("\n");
        content.append(centerText("دسوق - شارع المحرقه امام الحج خميس برل", 32)).append("\n");
        content.append(centerText("     01221005954         ", 32));

        saveToTextFile(content.toString(), invoiceNumber);

        // الطباعة على POS-80
        printToPOS80(content.toString());
    }

    // Function عشان نحط النص في النص
    private static String centerText(String text, int width) {
        if (text.length() >= width) {
            return text;
        }
        int padding = (width - text.length()) / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < padding; i++) {
            sb.append(" ");
        }
        sb.append(text);
        return sb.toString();
    }

    private static void printToPOS80(String content) {
        Platform.runLater(() -> {
            try {
                Printer printer = Printer.getDefaultPrinter();
                if (printer == null) {
                    AlertUtil.showError("خطأ", "لم يتم العثور على طابعة افتراضية");
                    return;
                }

                // إنشاء ورق مخصص للـ POS-80 (80mm width)
                Paper customPaper = createCustomPaper();

                PageLayout pageLayout = printer.createPageLayout(
                        customPaper,
                        PageOrientation.PORTRAIT,
                        Printer.MarginType.HARDWARE_MINIMUM
                );

                // إنشاء TextFlow للطباعة
                Text text = new Text(content);
                text.setFont(Font.font("Courier New", FontWeight.BOLD, 11));

                TextFlow textFlow = new TextFlow(text);
                textFlow.setTextAlignment(TextAlignment.CENTER);
                textFlow.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
                textFlow.setLineSpacing(2);

                // نطبع النص فقط (بدون QR)
                javafx.scene.layout.VBox contentBox = new javafx.scene.layout.VBox(textFlow);
                contentBox.setAlignment(javafx.geometry.Pos.CENTER);
                contentBox.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

                double printableWidth = pageLayout.getPrintableWidth();
                contentBox.setPrefWidth(printableWidth - 10);
                contentBox.setMaxWidth(printableWidth - 10);

                PrinterJob job = PrinterJob.createPrinterJob(printer);
                if (job != null) {
                    job.getJobSettings().setPageLayout(pageLayout);
                    boolean success = job.printPage(contentBox);
                    if (success) {
                        job.endJob();
                        System.out.println("تمت الطباعة بنجاح (بدون QR)");
                    } else {
                        AlertUtil.showError("فشل", "فشلت عملية الطباعة");
                    }
                } else {
                    AlertUtil.showError("خطأ", "لا يمكن إنشاء مهمة طباعة");
                }

            } catch (Exception e) {
                e.printStackTrace();
                AlertUtil.showError("خطأ", "حدث خطأ أثناء الطباعة: " + e.getMessage());
            }
        });
    }

    private static Paper createCustomPaper() {
        try {
            // محاولة إنشاء ورق مخصص 80mm × 297mm (طول A4)
            Constructor<Paper> constructor = Paper.class.getDeclaredConstructor(
                    String.class, double.class, double.class, Units.class
            );
            constructor.setAccessible(true);
            return constructor.newInstance("POS-80", 80.0, 297.0, Units.MM);
        } catch (Exception e) {
            System.out.println("تعذر إنشاء ورق مخصص، استخدام A4: " + e.getMessage());
            // استخدام A4 كبديل
            return Paper.A4;
        }
    }

    public static void SaveInvoice(int invoiceNumber, List<PurchaseProductDTO> items,
                                   double totalAfterDiscount, double discountAmount, String customerName,
                                   String phone, double paidAmount, double remainingAmount) {
        StringBuilder content = new StringBuilder();
        content.append("===========================\n");
        content.append("         🧾 فاتورة الشراء رقم ").append(invoiceNumber).append("     \n");
        content.append("===========================\n");

        if (!isBlank(customerName)) {
            content.append(String.format(" الزبون: %s\n", customerName.trim()));
        }
        if (!isBlank(phone)) {
            content.append(String.format(" الهاتف: %s\n", phone.trim()));
        }

        LocalDate today = LocalDate.now();
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        content.append(String.format(" التاريخ: %s   الساعة: %s\n", today, time));
        content.append("\n");

        List<PurchaseProductDTO> reversed = new ArrayList<>(items);
        Collections.reverse(reversed);

        double totalBeforeDiscount = 0;
        for (PurchaseProductDTO item : reversed) {
            if (item.getQuantity() > 0) {
                content.append(String.format("• %-12s × %d   =  %6.2f جنيه\n", item.getName(), item.getQuantity(), item.getSubtotal()));
                totalBeforeDiscount += item.getSubtotal();
            }
        }

        double calculatedDiscountAmount = totalBeforeDiscount - totalAfterDiscount;
        double discountPercentage = totalBeforeDiscount > 0 ? (calculatedDiscountAmount / totalBeforeDiscount) * 100 : 0;

        content.append("\n---------------------------\n");
        if (calculatedDiscountAmount > 0) {
            content.append(String.format("     الخصم: -%.2f جنيه (%.0f%%)\n", calculatedDiscountAmount, discountPercentage));
        }
        content.append(String.format("الإجمالي بعد الخصم:%10.2f جنيه\n", totalAfterDiscount));

        if (paidAmount > 0) {
            content.append(String.format(" المدفوع: %.2f جنيه\n", paidAmount));
        }
        if (remainingAmount > 0) {
            content.append(String.format(" الباقي (دين): %.2f جنيه\n", remainingAmount));
        }

        content.append("===========================\n");

        saveToTextFile(content.toString(), invoiceNumber);

        TextArea invoiceArea = new TextArea(content.toString());
        invoiceArea.setFont(Font.font("Courier New", FontWeight.BOLD, 14));
        invoiceArea.setWrapText(true);
    }

    private static void saveToTextFile(String content, int invoiceNumber) {
        try {
            String folderPath = "invoices";
            File folder = new File(folderPath);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
            String fileName = folderPath + "/invoice_" + invoiceNumber + "_" + timestamp + ".txt";

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
                writer.write(content);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static String getF() {
        return "AhmedSelimHossam";
    }
}
