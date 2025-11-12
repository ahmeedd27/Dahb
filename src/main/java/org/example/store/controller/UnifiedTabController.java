package org.example.store.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import org.example.store.model.Role;
import org.example.store.utils.AlertUtil;
import org.example.store.utils.Session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UnifiedTabController {

    @FXML
    private TabPane mainTabPane;

    // تعريف كل الـ Tabs المتاحة في النظام
    private static class TabDefinition {
        String title;
        String fxmlPath;
        List<Role> allowedRoles;

        TabDefinition(String title, String fxmlPath, Role... roles) {
            this.title = title;
            this.fxmlPath = fxmlPath;
            this.allowedRoles = Arrays.asList(roles);
        }

        boolean isAllowedFor(Role role) {
            return allowedRoles.contains(role);
        }
    }

    // قائمة بكل الشاشات والصلاحيات المطلوبة
    private static final List<TabDefinition> ALL_TABS = Arrays.asList(
            new TabDefinition("البيع", "/org/example/store/purchase-view.fxml",
                    Role.ADMIN, Role.MANAGER, Role.CASHIER),

            new TabDefinition("اداره المنتجات", "/org/example/store/product-view.fxml",
                    Role.ADMIN, Role.MANAGER),
            new TabDefinition("المصروفات", "/org/example/store/expenses.fxml",
                    Role.ADMIN, Role.MANAGER, Role.CASHIER),
            new TabDefinition("اداره المستخدمين", "/org/example/store/userManagement.fxml",
                    Role.ADMIN)
    );

    @FXML
    public void initialize() {
        buildTabsForCurrentUser();
        setupTabSelectionSecurity();
    }

    /**
     * بناء الـ Tabs حسب صلاحيات المستخدم الحالي
     */
    private void buildTabsForCurrentUser() {
        Role currentRole = Session.getRole();

        if (currentRole == null) {
            AlertUtil.showWarning("خطأ", "لم يتم تسجيل الدخول بشكل صحيح");
            return;
        }

        mainTabPane.getTabs().clear();

        for (TabDefinition tabDef : ALL_TABS) {
            if (tabDef.isAllowedFor(currentRole)) {
                Tab tab = createTab(tabDef);
                mainTabPane.getTabs().add(tab);
            }
        }
    }

    /**
     * إنشاء Tab من التعريف
     */
    private Tab createTab(TabDefinition tabDef) {
        Tab tab = new Tab(tabDef.title);
        tab.setClosable(false);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(tabDef.fxmlPath));
            Object content = loader.load(); // استقبال أي نوع من الـ layout
            if (content instanceof javafx.scene.Parent) {
                tab.setContent((javafx.scene.Parent) content);
            }
        } catch (IOException e) {
            e.printStackTrace();
            javafx.scene.control.Label errorLabel = new javafx.scene.control.Label("خطأ في تحميل الصفحة: " + tabDef.title);
            tab.setContent(new BorderPane(errorLabel));
        }

        return tab;
    }

    /**
     * حماية إضافية: منع الوصول للـ Tabs عن طريق الكود
     */
    private void setupTabSelectionSecurity() {
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                String tabTitle = newTab.getText();
                Role currentRole = Session.getRole();

                // التحقق من أن الـ Tab مسموح به
                boolean allowed = ALL_TABS.stream()
                        .filter(def -> def.title.equals(tabTitle))
                        .anyMatch(def -> def.isAllowedFor(currentRole));

                if (!allowed) {
                    AlertUtil.showWarning("صلاحيه غير صحيحه",
                            "ليست لديك الصلاحيه الصحيحه للدخول الي هذه الصفحه.");
                    // الرجوع للـ Tab السابق
                    mainTabPane.getSelectionModel().select(oldTab);
                }
            }
        });
    }


    public void selectTab(String tabTitle) {
        for (Tab tab : mainTabPane.getTabs()) {
            if (tab.getText().equals(tabTitle)) {
                mainTabPane.getSelectionModel().select(tab);
                return;
            }
        }
    }
}