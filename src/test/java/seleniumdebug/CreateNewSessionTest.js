const {Builder} = require('selenium-webdriver');
const chrome = require('selenium-webdriver/chrome');
// run  npm install selenium-webdriver chai mocha  to install dependencies
//run  npx mocha src/test/java/seleniumdebug/CreateNewSessionTest.js to execute the test
describe('CreateNewSessionTest', function () {

    it('should create a new session', async function () {
        this.timeout(60000);
        debugger;
        const {expect} = await import('chai');
        const options = new chrome.Options();
        options.enableBidi();

        let driver;
        try {
            driver = await new Builder()
                .forBrowser('chrome')
                .setChromeOptions(options)
                .usingServer('http://localhost:4444')
                .build();
            expect(driver).to.exist;
        } finally {
            if (driver) {
                await driver.quit();
            }
        }
    });
});
