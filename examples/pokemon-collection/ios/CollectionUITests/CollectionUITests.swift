import XCTest

final class CollectionUITests: XCTestCase {
    @MainActor private func launch() -> XCUIApplication {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["--ui-test-storage", UUID().uuidString]
        app.launch()
        return app
    }

    @MainActor private func capture(_ name: String, app: XCUIApplication) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    @MainActor private func search(_ text: String, in app: XCUIApplication) {
        let field = app.textFields["Search catalog"]
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        field.tap()
        let old = (field.value as? String) ?? ""
        if !old.isEmpty { field.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: old.count)) }
        field.typeText(text)
        app.buttons["Search"].firstMatch.tap()
    }

    @MainActor func testAddSearchWishlistRelaunchAndConfirmedDeletion() {
        let app = launch()
        XCTAssertTrue(app.staticTexts["0 cards · 0 copies"].waitForExistence(timeout: 5))
        capture("01-empty-collection", app: app)
        app.buttons["Find your first card"].tap()
        search("unknown", in: app)
        XCTAssertTrue(app.staticTexts["No cards found"].exists)
        search("25", in: app)
        XCTAssertTrue(app.buttons["card-sv03.5-025"].waitForExistence(timeout: 5))
        capture("02-catalog-search", app: app)
        app.buttons["card-sv03.5-025"].tap()
        app.buttons["Add to wishlist"].tap()
        XCTAssertTrue(app.buttons["Remove from wishlist"].exists)
        app.buttons["Add a copy"].tap()
        app.buttons["Quantity-Increment"].tap()
        app.buttons["finish-picker"].tap()
        app.buttons["Reverse holo"].tap()
        app.buttons["condition-picker"].tap()
        app.buttons["Lightly played"].tap()
        let note = app.textFields["Note (optional)"]
        note.tap()
        note.typeText("Gift from a friend")
        app.buttons["Done"].firstMatch.tap()
        capture("03-add-copy", app: app)
        app.buttons["save-copy"].tap()
        XCTAssertTrue(app.alerts["Added to collection"].waitForExistence(timeout: 5))
        app.alerts.buttons["Done"].tap()
        XCTAssertTrue(app.staticTexts["Gift from a friend"].exists)
        capture("04-card-details", app: app)
        app.terminate()
        app.launch()
        XCTAssertTrue(app.staticTexts["1 card · 2 copies"].waitForExistence(timeout: 5))
        capture("05-collection", app: app)
        app.buttons["card-sv03.5-025"].tap()
        XCTAssertTrue(app.staticTexts["Gift from a friend"].exists)
        XCTAssertTrue(app.staticTexts["2 copies · Reverse holo · Lightly played"].exists)
        app.buttons["Remove entry"].tap()
        app.alerts.buttons["Cancel"].tap()
        XCTAssertTrue(app.staticTexts["Gift from a friend"].exists)
        app.buttons["Remove entry"].tap()
        app.alerts.buttons["Remove"].tap()
        XCTAssertTrue(app.staticTexts["No copies yet"].waitForExistence(timeout: 5))
        app.navigationBars.buttons.element(boundBy: 0).tap()
        XCTAssertTrue(app.staticTexts["0 cards · 0 copies"].exists)
        app.tabBars.buttons["Wishlist"].tap()
        XCTAssertTrue(app.buttons["card-sv03.5-025"].waitForExistence(timeout: 5))
        capture("06-wishlist", app: app)
        app.buttons["card-sv03.5-025"].tap()
        app.buttons["Remove from wishlist"].tap()
        app.terminate()
        app.launch()
        app.tabBars.buttons["Wishlist"].tap()
        XCTAssertTrue(app.staticTexts["Your wishlist is empty"].waitForExistence(timeout: 5))
    }

    @MainActor func testCancelEveryCardAndLargeTextDarkAppearance() {
        let app = launch()
        app.buttons["Find your first card"].tap()
        search("  pikACHu ", in: app)
        app.buttons["card-sv03.5-025"].tap()
        app.buttons["Add a copy"].tap()
        app.buttons["Cancel"].tap()
        XCTAssertTrue(app.staticTexts["No copies yet"].exists)
        app.terminate()
        app.launchArguments += ["-UIPreferredContentSizeCategoryName", "UICTContentSizeCategoryAccessibilityXXXL", "--ui-test-dark"]
        app.launch()
        XCTAssertTrue(app.staticTexts["0 cards · 0 copies"].waitForExistence(timeout: 5))
        app.swipeUp()
        app.swipeUp()
        capture("07-empty-large-dark", app: app)
        app.buttons["Find your first card"].tap()
        for number in ["1", "4", "7", "25", "199", "200", "205"] {
            search(number, in: app)
            let id = String(format: "card-sv03.5-%03d", Int(number)!)
            app.buttons[id].tap()
            XCTAssertTrue(app.buttons["Add a copy"].exists)
            if number == "205" {
                capture("08-details-large-dark", app: app)
                let add = app.buttons["Add a copy"]
                for _ in 0..<8 where !add.isHittable { app.swipeUp() }
                XCTAssertTrue(add.isHittable)
                add.tap()
                capture("09-form-large-dark", app: app)
                let save = app.buttons["save-copy"]
                for _ in 0..<8 where !save.isHittable { app.swipeUp() }
                XCTAssertTrue(save.isHittable)
                save.tap()
                XCTAssertTrue(app.alerts["Added to collection"].waitForExistence(timeout: 5))
                app.alerts.buttons["Done"].tap()
            } else {
                app.navigationBars.buttons.element(boundBy: 0).tap()
            }
        }
        app.terminate()
        app.launch()
        XCTAssertTrue(app.staticTexts["1 card · 1 copy"].waitForExistence(timeout: 5))
        app.swipeUp()
        capture("10-collection-large-dark", app: app)
    }
}
