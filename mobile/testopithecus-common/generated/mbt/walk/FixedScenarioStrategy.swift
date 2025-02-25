// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mbt/walk/fixed-scenario-strategy.ts >>>

import Foundation

open class FixedScenarioStrategy: WalkStrategy {
  private var position: Int32 = 0
  private var scenario: YSArray<MBTAction>
  public init(_ scenario: YSArray<MBTAction>) {
    self.scenario = scenario
  }

  @discardableResult
  open func nextAction(_ _model: App, _ _applicationFeatures: YSArray<FeatureID>, _ _component: MBTComponent) throws -> MBTAction! {
    if self.position == self.scenario.length {
      return nil
    }
    let action = self.scenario[self.position]
    self.position += 1
    return action
  }

}

open class TestPlan {
  private var actions: YSArray<MBTAction> = YSArray()
  private init() {
  }

  @discardableResult
  open class func empty() -> TestPlan {
    return TestPlan()
  }

  @discardableResult
  open func then(_ action: MBTAction) -> TestPlan {
    return self.thenChain(YSArray(action))
  }

  @discardableResult
  open func thenChain(_ actions: YSArray<MBTAction>) -> TestPlan {
    for action in actions {
      self.actions.push(action)
      self.actions.push(DebugDumpAction())
    }
    return self
  }

  @discardableResult
  open func unsupportedActions(_ modelFeatures: YSArray<FeatureID>, _ applicationFeatures: YSArray<FeatureID>) -> YSArray<MBTAction> {
    let result: YSArray<MBTAction> = YSArray()
    for action in self.actions {
      if !action.supported(modelFeatures, applicationFeatures) {
        result.push(action)
      }
    }
    return result
  }

  @discardableResult
  open func getExpectedEvents() -> YSArray<EventusEvent> {
    let result: YSArray<EventusEvent> = YSArray()
    for action in self.actions {
      for event in action.events() {
        result.push(event)
      }
    }
    return result
  }

  @discardableResult
  open func isActionInTestPlan(_ type: String) -> Bool {
    for action in self.actions {
      if action.getActionType() == type {
        return true
      }
    }
    return false
  }

  @discardableResult
  open func build(_ accountLocks: YSArray<UserLock>!, _ assertionsEnabled: Bool = true) -> WalkStrategy {
    let actions: YSArray<MBTAction> = YSArray()
    for action in self.actions {
      if assertionsEnabled || AssertAction.type != action.getActionType() {
        actions.push(action)
      }
      if accountLocks != nil {
        accountLocks!.forEach({
          (accLock) in
          actions.push(PingAccountLockAction(accLock))
        })
      }
    }
    if !self.isActionInTestPlan("AssertSnapshotAction") {
      actions.push(AssertAction())
    }
    return FixedScenarioStrategy(actions)
  }

  @discardableResult
  open func tostring() -> String {
    var s = "PATH\n"
    for action in self.actions {
      s += action.tostring() + "\n"
    }
    return s + "END\n"
  }

}

open class TestopithecusTestRunner<T: AccountDataPreparer> {
  public let oauthService: OauthService
  public var reporter: ReportIntegration!
  private var locks: YSArray<UserLock> = YSArray()
  private var modelProvider: AppModelProvider! = nil
  private var testPlan: TestPlan! = nil
  private var userAccountsMap: UserServiceEnsemble
  private var startTime: Int64
  private var platform: MBTPlatform
  private let test: MBTTest<T>
  private let testsRegistry: TestsRegistry<T>
  private var preparerProvider: AccountDataPreparerProvider<T>
  private var network: SyncNetwork
  private var jsonSerializer: JSONSerializer
  private var logger: Logger
  private var assertionsEnabled: Bool
  private var userTags: YSArray<String>
  public var statToken: String
  public var testPalmToken: String
  private var tusConsumer: String
  public init(_ platform: MBTPlatform, _ test: MBTTest<T>, _ testsRegistry: TestsRegistry<T>, _ preparerProvider: AccountDataPreparerProvider<T>, _ applicationCredentials: OAuthApplicationCredentialsRegistry, _ network: SyncNetwork, _ jsonSerializer: JSONSerializer, _ logger: Logger, _ assertionsEnabled: Bool = true, _ userTags: YSArray<String> = YSArray(), _ statToken: String = "", _ testPalmToken: String = "", _ tusConsumer: String = "testopithecus", _ oauthHostConfig: OauthHostsConfig = OauthHostsConfig()) {
    self.platform = platform
    self.test = test
    self.testsRegistry = testsRegistry
    self.preparerProvider = preparerProvider
    self.network = network
    self.jsonSerializer = jsonSerializer
    self.logger = logger
    self.assertionsEnabled = assertionsEnabled
    self.userTags = userTags
    self.statToken = statToken
    self.testPalmToken = testPalmToken
    self.tusConsumer = tusConsumer
    let userService = UserService(network, jsonSerializer, logger)
    self.userAccountsMap = UserServiceEnsemble(userService, self.test.requiredAccounts(), self.tusConsumer, self.userTags)
    self.oauthService = OauthService(applicationCredentials, network, jsonSerializer, oauthHostConfig)
    self.startTime = int64(0)
    self.reporter = nil
  }

  @discardableResult
  open func isEnabled(_ modelFeatures: YSArray<FeatureID>, _ applicationFeatures: YSArray<FeatureID>) -> Bool {
    return self.testsRegistry.isTestEnabled(self.test, self.platform, modelFeatures, applicationFeatures)
  }

  @discardableResult
  open func isPassed() -> Bool {
    let passed = self.testsRegistry.isPassed(self.test)
    if passed {
      self.logger.info("Тест '\(self.test.description)' уже пройден, поэтому сейчас он пройден не будет")
    }
    return passed
  }

  open func clearTestResults() -> Void {
    self.testsRegistry.clearTestResults()
  }

  @discardableResult
  open func isNeedTryMore() -> Bool {
    let isNeedTryMore = self.testsRegistry.isNeedTryMore(self.test)
    if !isNeedTryMore {
      self.logger.info("Тест '\(self.test.description)' упал слишком много раз, ретраев не осталось")
    }
    return isNeedTryMore
  }

  open func failed() -> Void {
    self.testsRegistry.failed(self.test)
    var isIntermediate = false
    if self.isNeedTryMore() {
      isIntermediate = true
    }
    self.setTestResult(false, true, false, isIntermediate)
  }

  @discardableResult
  open func lockAndPrepareAccountData() -> XPromise<YSArray<OAuthUserAccount>> {
    self.logger.info("Try to prepare account data for test \(self.test.description)")
    let min10 = int64(10 * 60 * 1000)
    let min2 = int64(2 * 60 * 1000)
    let requiredAccounts = self.test.requiredAccounts()
    let accounts: YSArray<OAuthUserAccount> = YSArray()
    for i in stride(from: 0, to: requiredAccounts.length, by: 1) {
      let type = requiredAccounts[i]
      let lock: UserLock! = self.userAccountsMap.getAccountByType(type).tryAcquire(min10, min2)
      if lock == nil {
        self.releaseLocks()
        return reject(LockAcquireError(type))
      }
      self.locks.push(lock!)
    }
    let accountDataPreparers: YSArray<T> = YSArray()
    for accountIndex in stride(from: 0, to: self.locks.length, by: 1) {
      let account = self.locks[accountIndex].lockedAccount()
      let preparer = self.preparerProvider.provide(account, requiredAccounts[accountIndex])
      accountDataPreparers.push(preparer)
    }
    self.test.prepareAccounts(accountDataPreparers)
    let promises: YSArray<XPromise<Void>> = YSArray()
    for i in stride(from: 0, to: self.locks.length, by: 1) {
      let account = self.locks[i].lockedAccount()
      let accountType = requiredAccounts[i]
      var token: String!
      do {
        token = (try self.oauthService.getToken(account, accountType))
      } catch {
        let e = error
        return reject(getYSError(e))
      }
      let oauthAccount = OAuthUserAccount(account, token, accountType)
      accounts.push(oauthAccount)
      let preparePromise = accountDataPreparers[i].prepare(oauthAccount)
      promises.push(preparePromise)
    }
    self.modelProvider = self.preparerProvider.provideModelDownloader(accountDataPreparers, accounts)
    return all(promises).then({
      (_) in
      accounts
    })
  }

  @discardableResult
  open func runTest(_ accounts: YSArray<OAuthUserAccount>, _ start: MBTComponent, _ application: App!) throws -> Void {
    self.logger.info("Test \(self.test.description) started")
    self.startTime = int64(YSDate().getTime())
    self.setTestResult(true, false, false, false)
    self.setInfoForReporter()
    let modelProvider = requireNonNull(self.modelProvider, "Should lockAndPrepareAccountData before runTest!")
    let model = modelProvider.takeAppModel()
    let model2 = model.copy()
    let model3 = model.copy()
    let supportedFeatures = (application != nil ? application! : model).supportedFeatures
    let testPlan = self.test.scenario(accounts.map({
      (a) in
      a.account
    }), model, supportedFeatures)
    self.testPlan = testPlan
    self.logger.info(testPlan.tostring())
    let walkStrategyWithState1 = testPlan.build(nil, true)
    let walkStrategyWithState2 = testPlan.build(self.locks, self.assertionsEnabled)
    self.logger.info("Model vs Model testing started")
    if !self.testsRegistry.isIgnoredVsModelTesting(self.test) {
      let modelVsModel = StateMachine(model2, model3, walkStrategyWithState1, self.logger)
      (try modelVsModel.go(start))
    }
    self.logger.info("Model vs Model testing finished")
    self.logger.info("\n")
    if application == nil {
      return
    }
    self.logger.info("Model vs Application testing started")
    let modelVsApplication = StateMachine(model, application, walkStrategyWithState2, self.logger)
    (try modelVsApplication.go(start))
    self.logger.info("Model vs Application testing finished")
    self.logger.info("\n")
    self.logger.info("Test \(self.test.description) finished")
    self.testsRegistry.passed(self.test)
    self.setTestResult(false, false, false, false)
  }

  @discardableResult
  open func validateLogs(_ logs: String) throws -> Void {
    let testPlan = requireNonNull(self.testPlan, "Запустите тест runTest сначала!")
    let actualEvents: YSArray<EventusEvent> = YSArray()
    let lines = logs.split("\n")
    for line in lines {
      if line.length > 0 {
        let parser = CrossPlatformLogsParser(self.jsonSerializer)
        let event = parser.parse(line)
        if !self.testsRegistry.isIgnoredLogEvent(event.name) {
          actualEvents.push(event)
        }
      }
    }
    if self.testsRegistry.isIgnoredLogsTesting(self.test) {
      self.logger.info("TODO: пожалуйста, почините логирование для теста '\(self.test.description)'")
      return
    }
    let expectedEvents = testPlan.getExpectedEvents()
    let expectedEventsNames = expectedEvents.map({
      (event) in
      event.name
    })
    let actualEventsNames = actualEvents.map({
      (event) in
      event.name
    })
    let logMessage = "\nОжидаемые события '\(expectedEventsNames)'\nПолученные события '\(actualEventsNames)'"
    self.logger.info(logMessage)
    for i in stride(from: 0, to: maxInt32(actualEvents.length, expectedEvents.length), by: 1) {
      if i >= actualEvents.length {
        throw YSError("Ожидалось событие '\(expectedEvents[i].name)', но событий больше нет.'\(logMessage)'")
      }
      let actual = actualEvents[i]
      if i >= expectedEvents.length {
        throw YSError("Вижу событие '\(actual.name)', но больше событий не должно быть.'\(logMessage)'")
      }
      let expected = expectedEvents[i]
      self.logger.info("Событие №\(i): согласно действиям в сценарии должно быть '\(expected.name)', в логах '\(actual.name)'.'\(logMessage)'")
      (try assertStringEquals(expected.name, actual.name, "Разные события на месте #\(i).'\(logMessage)'\n"))
    }
  }

  open func finish() -> Void {
    self.releaseLocks()
    self.logger.info("Тест '\(self.test.description)' закончился")
  }

  open func sendTestsResults(_ testNameWhenSend: String) -> Void {
    if self.statToken == "" {
      self.logger.info("No token for stat! No statistics will be sent")
      return
    }
    if testNameWhenSend == self.test.description {
      let result: Result<String> = self.network.syncExecuteWithRetries(2, "https://upload.stat.yandex-team.ru/", StatNetworkRequest(self.testsRegistry.getTestResults()), self.statToken)
      if result.isError() {
        self.logger.info("Sending test results is failed with error \(result.getError().message)")
      }
      if result.isValue() {
        self.logger.info("Sending tests result is succeed, response \(result.getValue())")
      }
      self.testsRegistry.clearTestResults()
    } else {
      self.logger.info("Тест не последний в бакете, статистику не отправляем")
    }
  }

  open func setInfoForReporter() -> Void {
    if self.reporter != nil {
      self.reporter!.addTestpalmId(self.testsRegistry.getTestSettings(self.test).getCaseIDForPlatform(self.platform))
      let indexOfFeatureEnd: Int32 = self.test.description.indexOf(".")
      if indexOfFeatureEnd > 0 {
        self.reporter!.addFeatureName(self.test.description.substring(0, indexOfFeatureEnd))
      }
    }
  }

  private func releaseLocks() -> Void {
    if self.locks.length != 0 {
      self.locks.forEach({
        (lock) in
        if lock != nil {
          lock.release()
        }
      })
      self.locks = YSArray()
      self.logger.info("Locks for test \(self.test.description) released")
    }
  }

  private func setTestResult(_ isStarted: Bool, _ isFailed: Bool, _ isSkipped: Bool, _ isIntermediate: Bool) -> Void {
    let execTime: Int32 = int64ToInt32((int64(YSDate().getTime()) - self.startTime) / int64(1000))
    let caseId: Int32 = self.testsRegistry.getTestSettings(self.test).getCaseIDForPlatform(self.platform)
    let year = YSDate().getFullYear()
    let month = YSDate().getMonth() + 1
    let day = YSDate().getDate()
    let hours = YSDate().getHours()
    let minutes = YSDate().getMinutes()
    let seconds = YSDate().getSeconds()
    var project = ""
    switch self.platform {
      case MBTPlatform.MobileAPI:
        project = "mob_api"
      case MBTPlatform.Desktop:
        project = "desktop"
      case MBTPlatform.Android:
        project = "android"
      case MBTPlatform.IOS:
        project = "ios"
    }
    self.testsRegistry.setTestResult(MapJSONItem().putString("fielddate", "\(year)-\(month)-\(day) \(hours):\(minutes):\(seconds)").putString("project", project).putInt32("id", caseId).putString("test_name", self.test.description).putInt32("is_started", isStarted ? 1 : 0).putInt32("is_passed", self.isPassed() ? 1 : 0).putInt32("is_failed", isFailed ? 1 : 0).putInt32("is_skipped", isSkipped ? 1 : 0).putInt32("is_intermediate", isIntermediate ? 1 : 0).putString("execution_time", execTime > 5 ? int32ToString(execTime) : ""))
  }

}

public protocol AppModel: App {
  @discardableResult
  func copy() -> AppModel
  @discardableResult
  func getCurrentStateHash() -> Int64
}

public protocol AppModelProvider {
  @discardableResult
  func takeAppModel() -> AppModel
}

open class LockAcquireError: YSError {
  public init(_ accountType: AccountType2) {
    super.init("Can\'t acquire lock for \(accountType)!")
  }

}

