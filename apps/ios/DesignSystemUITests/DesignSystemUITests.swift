import XCTest

final class DesignSystemUITests: XCTestCase {
    @MainActor
    private func launch(_ scenario: String, largeRTL: Bool = false) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = ["--design-system-tests", "--ds-scenario=\(scenario)"]
        if largeRTL { app.launchArguments.append("--ds-large-rtl") }
        app.launch()
        return app
    }

    @MainActor
    func testLoadingButtonKeepsNameAndCannotActivate() {
        let app = launch("buttons")
        let save = app.buttons["save-action"]
        XCTAssertTrue(save.waitForExistence(timeout: 5))
        XCTAssertEqual(save.label, "Save")
        XCTAssertFalse(save.isEnabled)
        XCTAssertEqual(save.value as? String, "Loading")
        save.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        XCTAssertEqual(app.staticTexts["activation-count"].label, "Count: 0")
        app.buttons["Finish loading"].tap()
        XCTAssertTrue(save.isEnabled)
        save.tap()
        XCTAssertEqual(app.staticTexts["activation-count"].label, "Count: 1")
    }

    @MainActor
    func testDisabledAndInheritedDisabledButtonsCannotActivate() {
        let app = launch("buttons")
        for label in ["Disabled", "Inherited disabled"] {
            let button = app.buttons[label]
            XCTAssertTrue(button.waitForExistence(timeout: 5))
            XCTAssertFalse(button.isEnabled)
            button.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        }
        XCTAssertEqual(app.staticTexts["activation-count"].label, "Count: 0")
    }

    @MainActor
    func testSecondaryAndDestructiveButtonsActivate() {
        let app = launch("buttons")
        app.buttons["Secondary"].tap()
        app.buttons["Delete"].tap()
        XCTAssertEqual(app.staticTexts["activation-count"].label, "Count: 2")
    }

    @MainActor
    func testFieldUsesCallerStateAndExposesError() {
        let app = launch("field")
        let field = app.textFields["Name"]
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        field.tap()
        field.typeText("Ada")
        XCTAssertEqual(field.value as? String, "Ada")
        XCTAssertEqual(app.staticTexts["field-value"].label, "Value: Ada")
        app.buttons["Set externally"].tap()
        XCTAssertEqual(field.value as? String, "Grace")
        app.buttons["Show error"].tap()
        XCTAssertTrue(app.staticTexts["Required"].exists)
        XCTAssertFalse(app.staticTexts["Helpful text"].exists)
        XCTAssertEqual(app.textFields.matching(identifier: "Name").count, 1)
        XCTAssertFalse(app.staticTexts["Name"].exists)
    }

    @MainActor
    func testStatusActionIsOwnedByTheCaller() {
        let app = launch("status")
        XCTAssertTrue(app.staticTexts["Could not load"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Try again"].exists)
        app.buttons["Retry"].tap()
        XCTAssertEqual(app.staticTexts["activation-count"].label, "Count: 1")
    }

    @MainActor
    func testFieldAcceptsTapsAcrossMinimumTouchTarget() {
        for direction: CGFloat in [-1, 1] {
            let app = launch("field")
            let field = app.textFields["Name"]
            XCTAssertTrue(field.waitForExistence(timeout: 5))
            let insetFromCenter = max(44, field.frame.height) / 2 - 1
            field.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
                .withOffset(CGVector(dx: 0, dy: direction * insetFromCenter)).tap()
            XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: 5))
            field.typeText("Ada")
            XCTAssertEqual(field.value as? String, "Ada")
            XCTAssertEqual(app.staticTexts["field-value"].label, "Value: Ada")
        }
    }

    @MainActor
    func testDisabledFieldsDoNotFocusFromExpandedTarget() {
        for scenario in ["field-disabled", "field-parent-disabled"] {
            let app = launch(scenario)
            let field = app.textFields["Name"]
            XCTAssertTrue(field.waitForExistence(timeout: 5))
            XCTAssertFalse(field.isEnabled)
            field.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
                .withOffset(CGVector(dx: 0, dy: -21)).tap()
            XCTAssertFalse(app.keyboards.firstMatch.exists)
            XCTAssertEqual(app.staticTexts["field-value"].label, "Value: ")
        }
    }

    @MainActor
    func testLargeTextRtlActionRemainsUsable() {
        let app = launch("buttons", largeRTL: true)
        let button = app.buttons["save-action"]
        XCTAssertTrue(button.waitForExistence(timeout: 5))
        XCTAssertGreaterThanOrEqual(button.frame.height, 44)
        XCTAssertEqual(button.label, "A longer action label that needs to wrap")
        app.buttons["Finish loading"].tap()
        XCTAssertTrue(button.isHittable)
        button.tap()
        XCTAssertEqual(app.staticTexts["activation-count"].label, "Count: 1")
    }
}
