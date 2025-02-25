// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/mailbox-preparer.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.eventus.*
import com.yandex.xplat.mapi.*
import com.yandex.xplat.testopithecus.common.*

public open class FolderSpec(var name: String, var messages: YSArray<MessageSpec>) {
}

public open class UserSpec(var email: String, var name: String) {
}

public open class AttachmentSpec(var title: String, var contentType: String, var contentBase64: String) {
    companion object {
        @JvmStatic
        open fun withName(title: String): AttachmentSpec {
            return AttachmentSpec(title, "application/octet-stream", "ZXhhbXBsZQ==")
        }

    }
}

public open class MessageSpec(builder: MessageSpecBuilder) {
    var sender: UserSpec
    var subject: String
    var textBody: String
    var timestamp: YSDate
    var toReceivers: YSArray<UserSpec>
    var labels: YSArray<LabelData>
    var systemLabels: YSArray<LabelType>
    var attachments: YSArray<AttachmentSpec>
    init {
        this.sender = requireNonNull(builder.sender, "Sender required!")
        this.subject = requireNonNull(builder.subject, "Subject required!")
        this.textBody = requireNonNull(builder.textBody, "Body text required!")
        this.timestamp = requireNonNull(builder.timestamp, "Timestamp required!")
        this.toReceivers = builder.toReceivers
        this.labels = builder.labels
        this.attachments = builder.attachments
        this.systemLabels = builder.systemLabels
    }
    companion object {
        @JvmStatic
        open fun builder(): MessageSpecBuilder {
            return MessageSpecBuilder()
        }

        @JvmStatic
        open fun create(subject: String, timestamp: YSDate? = null): MessageSpec {
            return this.builder().withSender(UserSpec("testbotauto@yandex.ru", "Other User")).withSubject(subject).withTextBody("first line").withTimestamp(if (timestamp != null) timestamp else YSDate("2019-07-20T17:03:06.000Z")).build()
        }

    }
}

public open class MessageSpecBuilder {
    var sender: UserSpec? = null
    var subject: String? = null
    var textBody: String? = null
    var timestamp: YSDate? = null
    var toReceivers: YSArray<UserSpec> = mutableListOf()
    var labels: YSArray<LabelData> = mutableListOf()
    var systemLabels: YSArray<LabelType> = mutableListOf()
    var attachments: YSArray<AttachmentSpec> = mutableListOf()
    open fun withSender(sender: UserSpec): MessageSpecBuilder {
        this.sender = sender
        return this
    }

    open fun withSubject(subject: String): MessageSpecBuilder {
        this.subject = subject
        return this
    }

    open fun withTextBody(textBody: String): MessageSpecBuilder {
        this.textBody = textBody
        return this
    }

    open fun withTimestamp(timestamp: YSDate): MessageSpecBuilder {
        this.timestamp = timestamp
        return this
    }

    open fun addReceiver(`receiver`: UserSpec): MessageSpecBuilder {
        this.toReceivers.add(`receiver`)
        return this
    }

    open fun addLabels(labels: YSArray<LabelData>): MessageSpecBuilder {
        labels.forEach( {
            label ->
            this.labels.add(label)
        })
        return this
    }

    open fun withSystemLabel(labelType: LabelType): MessageSpecBuilder {
        this.systemLabels.add(labelType)
        return this
    }

    open fun addAttachments(attachments: YSArray<AttachmentSpec>): MessageSpecBuilder {
        attachments.forEach( {
            attachment ->
            this.attachments.add(attachment)
        })
        return this
    }

    open fun withDefaults(): MessageSpecBuilder {
        this.sender = UserSpec("testbotauto@yandex.ru", "Other User")
        this.subject = "subj"
        this.textBody = "first line"
        this.timestamp = YSDate("2019-07-20T17:03:06.000Z")
        return this
    }

    open fun build(): MessageSpec {
        return MessageSpec(this)
    }

}

public open class FilterRule(builder: FilterRuleBuilder) {
    var conditions: YSArray<FilterCondition>
    var logic: FilterLogicType
    var actions: YSArray<FilterAction>
    var id: String?
    var name: String
    var attachment: FilterAttachmentType
    var letter: FilterLetterType
    var stop: Boolean
    var enabled: Boolean
    init {
        this.conditions = builder.conditions
        this.logic = builder.logic
        this.actions = builder.actions
        this.id = builder.id
        this.name = builder.name
        this.attachment = builder.attachment
        this.letter = builder.letter
        this.stop = builder.stop
        this.enabled = builder.enabled
    }
    companion object {
        @JvmStatic
        open fun builder(): FilterRuleBuilder {
            return FilterRuleBuilder()
        }

    }
}

public open class FilterRuleBuilder {
    var conditions: YSArray<FilterCondition> = mutableListOf()
    var logic: FilterLogicType = FilterLogicType.and
    var actions: YSArray<FilterAction> = mutableListOf()
    var id: String? = null
    var name: String = ""
    var attachment: FilterAttachmentType = FilterAttachmentType.all
    var letter: FilterLetterType = FilterLetterType.nospam
    var stop: Boolean = false
    var enabled: Boolean = true
    open fun setCondition(condition: FilterCondition): FilterRuleBuilder {
        this.conditions.add(condition)
        return this
    }

    open fun setLogic(logicType: FilterLogicType): FilterRuleBuilder {
        this.logic = logicType
        return this
    }

    open fun setAction(action: FilterAction): FilterRuleBuilder {
        this.actions.add(action)
        return this
    }

    open fun setId(id: String): FilterRuleBuilder {
        this.id = id
        return this
    }

    open fun setName(name: String): FilterRuleBuilder {
        this.name = name
        return this
    }

    open fun setAttachmentType(attachmentType: FilterAttachmentType): FilterRuleBuilder {
        this.attachment = attachmentType
        return this
    }

    open fun setLetter(letterType: FilterLetterType): FilterRuleBuilder {
        this.letter = letterType
        return this
    }

    open fun setStop(stop: Boolean): FilterRuleBuilder {
        this.stop = stop
        return this
    }

    open fun setEnable(enable: Boolean): FilterRuleBuilder {
        this.enabled = enable
        return this
    }

    open fun build(): FilterRule {
        return FilterRule(this)
    }

}

public open class MailAccountSpec(val login: String, val password: String, val host: String) {
    companion object {
        @JvmStatic
        open fun fromUserAccount(account: UserAccount, host: String): MailAccountSpec {
            return MailAccountSpec(account.login, account.password, host)
        }

    }
}

public open class PreparingMailbox(builder: MailboxBuilder) {
    var mailAccount: MailAccountSpec
    var folders: YSArray<FolderSpec> = mutableListOf()
    var labelsWithoutMessage: YSArray<LabelData> = mutableListOf()
    var zeroSuggests: YSArray<String> = mutableListOf()
    var filters: YSArray<FilterRule> = mutableListOf()
    var contacts: YSArray<Contact> = mutableListOf()
    var isTabEnabled: Boolean = false
    init {
        this.mailAccount = builder.mailAccount
        builder.folders.__forEach(__LBL__MailboxPreparer_1@ {
            messages, name ->
            this.folders.add(FolderSpec(name, messages))
        })
        this.isTabEnabled = builder.isTabEnabled
        builder.labelsWithoutMessage.forEach( {
            label ->
            this.labelsWithoutMessage.add(label)
        })
        builder.zeroSuggests.forEach( {
            query ->
            this.zeroSuggests.add(query)
        })
        builder.contacts.forEach( {
            contact ->
            this.contacts.add(contact)
        })
        builder.filters.forEach( {
            filter ->
            this.filters.add(filter)
        })
    }
}

public open class MailboxPreparerProvider(val platform: MBTPlatform, val jsonSerializer: JSONSerializer, val network: SyncNetwork, val logger: Logger, val sleep: SyncSleep, val imap: ImapProvider): AccountDataPreparerProvider<MailboxBuilder>() {
    open override fun provide(lockedAccount: UserAccount, type: AccountType2): MailboxBuilder {
        val mailAccount = MailAccountSpec.fromUserAccount(lockedAccount, this.getImapHost(type))
        return MailboxBuilder(mailAccount, this)
    }

    open override fun provideModelDownloader(fulfilledPreparers: YSArray<MailboxBuilder>, accountsWithTokens: YSArray<OAuthUserAccount>): AppModelProvider {
        val clients = accountsWithTokens.map( {
            accWithToken ->
            MailboxClient(this.platform, accWithToken, this.network, this.jsonSerializer, this.logger)
        })
        return MailboxDownloader(clients, this.logger)
    }

    open fun getOAuthAccount(account: UserAccount, type: AccountType2): OAuthUserAccount {
        val token = this.createOauthService().getToken(account, type)
        return OAuthUserAccount(account, token, type)
    }

    private fun createOauthService(): OauthService {
        return OauthService(PublicBackendConfig.mailApplicationCredentials, this.network, this.jsonSerializer)
    }

    private fun getImapHost(accountType: AccountType2): String {
        when (accountType) {
            AccountType2.Yandex -> {
                return "imap.yandex.ru"
            }
            AccountType2.YandexTeam -> {
                return "imap.yandex-team.ru"
            }
            AccountType2.Yahoo -> {
                return "imap.mail.yahoo.com"
            }
            AccountType2.Google -> {
                return "imap.google.com"
            }
            AccountType2.Mail -> {
                return "imap.mail.ru"
            }
            AccountType2.Rambler -> {
                return "imap.rambler.ru"
            }
            AccountType2.Hotmail -> {
                return "outlook.office365.com"
            }
            AccountType2.Outlook -> {
                return "outlook.office365.com"
            }
            AccountType2.Other -> {
            }
            else -> {
                return "imap.yandex.ru"
            }
        }
        return "imap.yandex.ru"
    }

}

public open class MailboxBuilder(val mailAccount: MailAccountSpec, private val `delegate`: MailboxPreparerProvider): AccountDataPreparer {
    val folders: YSMap<FolderName, YSArray<MessageSpec>> = mutableMapOf<FolderName, YSArray<MessageSpec>>()
    val labelsWithoutMessage: YSArray<LabelData> = mutableListOf()
    private val timestampProvider: MessageTimeProvider = MessageTimeProvider()
    val zeroSuggests: YSArray<String> = mutableListOf()
    val contacts: YSArray<Contact> = mutableListOf()
    val filters: YSArray<FilterRule> = mutableListOf()
    private var currentFolder: FolderName = DefaultFolderName.inbox
    var isTabEnabled: Boolean = false
    var sendMessagesViaApi: Boolean = false
    open fun nextMessage(subject: String): MailboxBuilder {
        val timestamp = this.timestampProvider.nextTime()
        this.addMessageToFolder(this.currentFolder, MessageSpec.create(subject, timestamp))
        return this
    }

    open fun nextManyMessage(size: Int): MailboxBuilder {
        for (i in (0 until size step 1)) {
            this.nextMessage("Message${i}")
        }
        return this
    }

    open fun nextCustomMessage(msg: MessageSpecBuilder): MailboxBuilder {
        val timestamp = this.timestampProvider.nextTime()
        this.addMessageToFolder(this.currentFolder, msg.withTimestamp(timestamp).build())
        return this
    }

    open fun nextThread(subject: String, threadSize: Int): MailboxBuilder {
        for (`_` in (0 until threadSize step 1)) {
            this.nextMessage(subject)
        }
        return this
    }

    open fun switchFolder(folderName: FolderName, parentFolders: YSArray<FolderName> = mutableListOf()): MailboxBuilder {
        this.currentFolder = formatFolderName(folderName, parentFolders)
        return this
    }

    open fun turnOnTab(): MailboxBuilder {
        this.isTabEnabled = true
        this.currentFolder = FolderBackendName.inbox
        return this
    }

    open fun sendMessageViaMobileApi(): MailboxBuilder {
        this.sendMessagesViaApi = true
        return this
    }

    open fun addMessageToFolder(folderName: FolderName, message: MessageSpec, parentFolders: YSArray<FolderName> = mutableListOf()): MailboxBuilder {
        this.createFolder(folderName, parentFolders)
        val folderMessage = this.folders.`get`(formatFolderName(folderName, parentFolders))!!
        folderMessage.add(message)
        return this
    }

    open fun createFolder(folderName: FolderName, parentFolders: YSArray<FolderName> = mutableListOf()): MailboxBuilder {
        val folder = formatFolderName(folderName, parentFolders)
        if (!this.folders.has(folder)) {
            this.folders.set(folder, mutableListOf())
        }
        return this
    }

    open fun createLabel(label: LabelData): MailboxBuilder {
        if (this.labelsWithoutMessage.filter( {
            labelData ->
            LabelData.matches(labelData, label)
        }).size == 0) {
            this.labelsWithoutMessage.add(label)
        }
        return this
    }

    open fun createContact(contact: Contact): MailboxBuilder {
        this.contacts.add(contact)
        return this
    }

    open fun createFilter(filter: FilterRule): MailboxBuilder {
        this.filters.add(filter)
        return this
    }

    open fun saveQueryToZeroSuggest(query: String): MailboxBuilder {
        if (!this.zeroSuggests.contains(query)) {
            this.zeroSuggests.add(query)
        }
        return this
    }

    open fun build(): PreparingMailbox {
        return PreparingMailbox(this)
    }

    open override fun prepare(account: OAuthUserAccount): XPromise<Unit> {
        try {
            WebApiRequest(account.type).enableImap(this.`delegate`.network, account.oauthToken)
        } catch (e: RuntimeException) {
            this.`delegate`.logger.error("Включение настройки IMAP завершилось с ошибкой ${e}")
        }
        val imap = this.`delegate`.imap.provide(this.mailAccount)
        val client = MailboxClient(this.`delegate`.platform, account, this.`delegate`.network, this.`delegate`.jsonSerializer, this.`delegate`.logger)
        var senderClient: MailboxClient? = null
        var senderAccount: OAuthUserAccount? = null
        if (this.sendMessagesViaApi) {
            try {
                senderAccount = this.`delegate`.getOAuthAccount(UserAccount("yndx-message-sender@yandex.ru", "qwerty123asdf"), AccountType2.Yandex)
            } catch (e: RuntimeException) {
                return reject(getYSError(e))
            }
            senderClient = MailboxClient(this.`delegate`.platform, senderAccount, this.`delegate`.network, this.`delegate`.jsonSerializer, this.`delegate`.logger)
        }
        val preparer = MailboxPreparer(imap, client, senderClient, this.`delegate`.sleep, this.`delegate`.logger)
        return preparer.prepare(this)
    }

}

public open class MailboxPreparer(private var imap: Imap, private var client: MailboxClient, private var senderClient: MailboxClient? = null, private var syncSleep: SyncSleep, private var logger: Logger) {
    init {
        client.logger.info("Используй меня полностью")
    }
    open fun prepare(builder: MailboxBuilder): XPromise<Unit> {
        val mailbox = builder.build()
        this.logger.info("Готовим ящик ${this.client.oauthAccount.account.login} / ${this.client.oauthAccount.account.password}")
        return this.connect().flatThen( {
            _ ->
            this.clearMailbox(mailbox.mailAccount)
        }).flatThen( {
            _ ->
            this.createMailbox(mailbox)
        }).flatThen( {
            _ ->
            this.disconnect()
        }).then( {
            _ ->
            this.setTabEnableState(mailbox.isTabEnabled)
        }).then( {
            _ ->
            this.waitForSync(mailbox)
        }).then( {
            _ ->
            this.adjustMailbox(mailbox)
        })
    }

    private fun waitForSync(mailbox: PreparingMailbox): Unit {
        val ttl = int64(5 * 60 * 1000)
        val deadline = currentTimeMs() + ttl
        while (currentTimeMs() < deadline) {
            if (this.isInSync(mailbox)) {
                return
            }
            this.syncSleep.sleepMs(5000)
        }
        fail("Не могу дождаться синхронизации ящика в течение ${ttl}мс")
    }

    private fun isInSync(mailbox: PreparingMailbox): Boolean {
        for (folder in mailbox.folders) {
            val fid = this.client.getFolderByName(this.getRelevantIfTab(folder.name), mailbox.isTabEnabled).fid
            val backendMessages = this.client.getMessagesInFolder(fid, folder.messages.size + 1, mailbox.isTabEnabled)
            val actualMessagesCount = backendMessages.size
            val expectedMessagesCount = folder.messages.size
            if (actualMessagesCount != expectedMessagesCount) {
                this.logger.info("Еще не засинкана папка ${folder.name}, жду ${expectedMessagesCount} писем, а там пока ${actualMessagesCount}")
                return false
            }
        }
        val foldersCount = mailbox.folders.size
        this.logger.info("Проверил ${foldersCount} папок, вроде ящик засинкан")
        return true
    }

    private fun clearMailbox(account: MailAccountSpec): XPromise<Unit> {
        this.logger.info("Clearing mailbox for ${account.login}")
        return this.imap.fetchAllFolders().flatThen( {
            folders ->
            this.clearFolders(folders)
        }).then( {
            _ ->
            this.deleteAllLabels()
        })
    }

    private fun clearFolders(folders: YSArray<ImapFolderDisplay>): XPromise<Unit> {
        folders.add(ImapFolderDisplay(DefaultFolderName.trash))
        folders.add(ImapFolderDisplay("Корзина"))
        var result = resolve(getVoid())
        for (folder in folders) {
            result = result.flatThen(__LBL__MailboxPreparer_2@ {
                _ ->
                return@__LBL__MailboxPreparer_2 this.hasFolder(folder.name).flatThen(__LBL__MailboxPreparer_3@ {
                    has ->
                    return@__LBL__MailboxPreparer_3 if (has) if (this.isDefaultFolder(folder.name)) this.clearFolder(folder.name) else this.deleteFolder(folder.name) else resolve(getVoid())
                })
            })
        }
        return result
    }

    private fun createMailbox(mailbox: PreparingMailbox): XPromise<Unit> {
        this.logger.info("Creating mailbox for ${mailbox.mailAccount.login}")
        return if (this.senderClient != null) this.createMailboxViaApi(mailbox) else this.alll(mailbox.folders.map( {
            f ->
            this.populateFolder(f)
        }))
    }

    private fun createMailboxViaApi(mailbox: PreparingMailbox): XPromise<Unit> {
        val self = this
        val email = formatLogin(self.client.oauthAccount.account.login)
        mailbox.folders.filter( {
            folder ->
            folder.name == DefaultFolderName.inbox
        })[0].messages.forEach(__LBL__MailboxPreparer_4@ {
            message ->
            self.senderClient!!.sendMessage(email, message.subject, message.textBody)
        })
        return resolve(getVoid())
    }

    private fun getMessagesMeta(mailbox: PreparingMailbox): YSArray<MessageMeta> {
        val messagesMeta: YSArray<MessageMeta> = mutableListOf()
        for (folder in mailbox.folders) {
            val fid = this.client.getFolderByName(folder.name, mailbox.isTabEnabled).fid
            this.client.getMessagesInFolder(fid, folder.messages.size + 1, mailbox.isTabEnabled).forEach( {
                m ->
                messagesMeta.add(m)
            })
        }
        return messagesMeta
    }

    private fun getMidsForInboxTab(mailbox: PreparingMailbox, tabName: FolderName): YSArray<ID> {
        val messages = mailbox.folders.filter( {
            folder ->
            folder.name == tabName
        })[0].messages
        val mids: YSArray<ID> = mutableListOf()
        this.client.getMessagesInFolder(int64(-10), messages.size + 1, mailbox.isTabEnabled).forEach( {
            m ->
            mids.add(m.mid)
        })
        return mids
    }

    private fun adjustMailbox(mailbox: PreparingMailbox): Unit {
        this.logger.info("Adjusting mailbox for ${mailbox.mailAccount.login} with backend operations")
        this.populateLabels(mailbox)
        this.prepareZeroSuggest(mailbox)
        this.prepareContacts(mailbox)
        this.prepareFilters(mailbox)
        mailbox.folders.map( {
            folder ->
            this.moveMessageToTabIfNeeded(folder.name, mailbox)
        })
    }

    private fun populateFolder(folder: FolderSpec): XPromise<Unit> {
        val name = this.getInboxIfTab(folder.name)
        val createFolder: XPromise<Unit> = if (this.isDefaultFolder(name)) this.imap.openFolder(name).then( {
            _ ->
            getVoid()
        }) else this.createFolder(name)
        return createFolder.flatThen( {
            _ ->
            this.alll(folder.messages.map( {
                msg ->
                this.createMessage(folder.name, msg)
            }))
        })
    }

    private fun populateLabels(mailbox: PreparingMailbox): Unit {
        val labelsToMsgTimestamps: YSMap<LabelName, YSArray<Long>> = mutableMapOf<LabelName, YSArray<Long>>()
        val labelToColor: YSMap<LabelName, String> = mutableMapOf<LabelName, String>()
        mailbox.folders.forEach( {
            folder ->
            folder.messages.forEach(__LBL__MailboxPreparer_5@ {
                msg ->
                msg.labels.forEach(__LBL__MailboxPreparer_6@ {
                    label ->
                    if (labelsToMsgTimestamps.has(label.name)) {
                        labelsToMsgTimestamps.`get`(label.name)!!.add(int64(msg.timestamp.getTime()))
                    } else {
                        labelsToMsgTimestamps.set(label.name, mutableListOf(int64(msg.timestamp.getTime())))
                    }
                    if (!labelToColor.has(label.name)) {
                        labelToColor.set(label.name, label.color)
                    }
                })
                msg.systemLabels.forEach(__LBL__MailboxPreparer_7@ {
                    labelType ->
                    val lid = this.client.getLabelList().filter( {
                        label ->
                        label.type == labelType
                    })[0].lid
                    val messagesMeta: YSArray<ID> = this.getMessagesMeta(mailbox).filter( {
                        message ->
                        int64(msg.timestamp.getTime()) == message.timestamp
                    }).map( {
                        message ->
                        message.mid
                    })
                    this.client.markMessagesWithLabel(messagesMeta, lid)
                })
            })
        })
        labelsToMsgTimestamps.__forEach(__LBL__MailboxPreparer_8@ {
            timestamps, labelName ->
            mailbox.labelsWithoutMessage = mailbox.labelsWithoutMessage.filter( {
                label ->
                label.name != labelName
            })
            this.createLabel(LabelData(labelName, labelToColor.`get`(labelName)!!))
            this.markMessagesWithLabel(LabelData(labelName), timestamps, mailbox)
        })
        mailbox.labelsWithoutMessage.forEach( {
            label ->
            this.createLabel(label)
        })
    }

    private fun prepareZeroSuggest(mailbox: PreparingMailbox): Unit {
        val currentZeroSuggest = this.getZeroSuggests()
        currentZeroSuggest.forEach( {
            query ->
            this.deleteQuery(query)
        })
        mailbox.zeroSuggests.forEach( {
            query ->
            this.saveQuery(query)
        })
    }

    private fun prepareFilters(mailbox: PreparingMailbox): Unit {
        val currentFilterIds = this.getFilters()
        currentFilterIds.forEach( {
            id ->
            this.deleteFilter(id)
        })
        mailbox.filters.forEach(__LBL__MailboxPreparer_9@ {
            filterData ->
            filterData.actions.forEach(__LBL__MailboxPreparer_10@ {
                action ->
                if (action.key == FilterActionType.applyLabel && action.value != null) {
                    val lid = this.client.getLabelByName(action.value!!).lid
                    action.value = lid
                }
                if (action.key == FilterActionType.moveToFolder && action.value != null) {
                    val fid = this.client.getFolderByName(action.value!!).fid
                    action.value = int64ToString(fid)
                }
            })
            val id = this.createFilter(filterData)
            if (!filterData.enabled) {
                this.disableFilter(id)
            }
        })
    }

    private fun prepareContacts(mailbox: PreparingMailbox): Unit {
        val contactsToDelete = this.getContactsIds()
        if (contactsToDelete.size > 0) {
            this.deleteContacts(contactsToDelete)
        }
        if (mailbox.contacts.size > 0) {
            this.createContacts(mailbox.contacts)
        }
    }

    private fun connect(): XPromise<Unit> {
        val self = this
        self.client.setParameter("enable_imap", "true")
        self.client.setParameter("enable_imap_auth_plain", "on")
        self.client.setParameter("disable_imap_autoexpunge", "")
        return promise(__LBL__MailboxPreparer_11@ {
            resolve, reject ->
            self.imap.connect(__LBL__MailboxPreparer_12@ {
                error ->
                self.handle(resolve, reject, error, "Connected!")
            })
        })
    }

    private fun disconnect(): XPromise<Unit> {
        val self = this
        return promise(__LBL__MailboxPreparer_13@ {
            resolve, reject ->
            self.imap.disconnect(__LBL__MailboxPreparer_14@ {
                error ->
                self.handle(resolve, reject, error, "Disconnected!")
            })
        })
    }

    private fun clearFolder(folder: String): XPromise<Unit> {
        this.logger.info("Clearing folder ${folder}")
        return this.imap.openFolder(folder).flatThen( {
            f ->
            this.deleteMessages(folder, f.messageCount)
        }).flatThen( {
            _ ->
            this.expungeFolder(folder)
        })
    }

    private fun hasFolder(folder: String): XPromise<Boolean> {
        return this.imap.fetchAllFolders().then( {
            folders ->
            folders.map( {
                f ->
                f.name
            }).contains(folder)
        })
    }

    private fun deleteMessages(folder: String, messageCount: Int): XPromise<Unit> {
        if (messageCount == 0) {
            return resolve(getVoid())
        }
        val self = this
        return promise(__LBL__MailboxPreparer_15@ {
            resolve, reject ->
            self.imap.deleteMessages(folder, messageCount, __LBL__MailboxPreparer_16@ {
                error ->
                self.handle(resolve, reject, error, "Deleted ${messageCount} messages from ${folder}")
            })
        })
    }

    private fun createFolder(folder: String): XPromise<Unit> {
        val self = this
        return promise(__LBL__MailboxPreparer_17@ {
            resolve, reject ->
            self.imap.createFolder(folder, __LBL__MailboxPreparer_18@ {
                error ->
                self.handle(resolve, reject, error, "[CREATE] Created folder ${folder}")
            })
        })
    }

    private fun setTabEnableState(enabled: Boolean): Unit {
        val self = this
        self.client.setParameter("show_folders_tabs", if (enabled) "on" else "")
    }

    private fun createLabel(label: LabelData): Unit {
        val self = this
        self.client.createLabel(label)
    }

    private fun deleteAllLabels(): Unit {
        val self = this
        self.client.getCustomUserLabelsList().forEach( {
            label ->
            self.client.deleteLabel(label.lid)
        })
    }

    private fun markMessagesWithLabel(labelData: LabelData, timestamps: YSArray<Long>, mailbox: PreparingMailbox): Unit {
        val self = this
        val labelToMark: Label = requireNonNull(undefinedToNull(this.client.getLabelList().filter( {
            label ->
            label.name!! == labelData.name
        }).pop()), "Не смог найти созданную метку с именем ${labelData.name}")
        val messagesMeta: YSArray<ID> = this.getMessagesMeta(mailbox).filter( {
            message ->
            timestamps.contains(message.timestamp)
        }).map( {
            message ->
            message.mid
        })
        self.client.markMessagesWithLabel(messagesMeta, labelToMark.lid)
    }

    private fun moveMessageToTabIfNeeded(tabName: FolderName, mailbox: PreparingMailbox): Unit {
        if (!isTab(tabName)) {
            return
        }
        val self = this
        val mids: YSArray<ID> = this.getMidsForInboxTab(mailbox, tabName)
        for (mid in mids) {
            self.client.moveMessageToFolder(mid, tabNameToFid(tabName))
        }
    }

    private fun createMessage(folder: String, message: MessageSpec): XPromise<Unit> {
        val folderName = this.getInboxIfTab(folder)
        val self = this
        return promise(__LBL__MailboxPreparer_19@ {
            resolve, reject ->
            self.imap.appendMessage(folderName, message, __LBL__MailboxPreparer_20@ {
                error ->
                self.handle(resolve, reject, error, "[APPEND] Created message in folder ${folderName}")
            })
        })
    }

    private fun deleteFolder(folder: String): XPromise<Unit> {
        val self = this
        return promise(__LBL__MailboxPreparer_21@ {
            resolve, reject ->
            self.imap.deleteFolder(folder, __LBL__MailboxPreparer_22@ {
                error ->
                self.handle(resolve, reject, error, "Deleted folder ${folder}")
            })
        })
    }

    private fun expungeFolder(folder: String): XPromise<Unit> {
        val self = this
        return promise(__LBL__MailboxPreparer_23@ {
            resolve, reject ->
            self.imap.expungeFolder(folder, __LBL__MailboxPreparer_24@ {
                error ->
                self.handle(resolve, reject, error, "Expunged folder ${folder}")
            })
        })
    }

    private fun getContactsIds(): YSArray<String> {
        val self = this
        return self.client.getContacts(GetAbookContactsRequestData(30, 0, AbookContactsSortType.alpha, null, null)).contacts.map( {
            contact ->
            int32ToString(contact.id)
        })
    }

    private fun deleteContacts(contactIds: YSArray<String>): Unit {
        val self = this
        self.client.deleteContacts(contactIds)
    }

    private fun createContacts(contacts: YSArray<Contact>): Unit {
        val self = this
        self.client.createContacts(CreateAbookContactsRequestData(contacts.map( {
            contact ->
            CreateAbookContactData(AbookContactName(contact.name, null, null, null, null), mutableListOf(contact.email), null, null, null, null)
        })))
    }

    private fun getZeroSuggests(): YSArray<String> {
        val self = this
        return self.client.getZeroSuggest().map( {
            suggest ->
            suggest.show_text
        })
    }

    private fun getFilters(): YSArray<String> {
        val self = this
        return self.client.listFilter().rules.map( {
            rule ->
            rule.id
        })
    }

    private fun deleteFilter(id: String): Unit {
        val self = this
        self.client.deleteFilter(id)
    }

    private fun createFilter(data: FilterRule): String {
        val self = this
        return self.client.createFilter(CreateUpdateFilterRuleRequestData(data.conditions, data.logic, data.actions, data.id, data.name, data.attachment, data.letter, data.stop)).id
    }

    private fun disableFilter(id: String): Unit {
        val self = this
        self.client.disableFilter(id)
    }

    private fun saveQuery(query: String): Unit {
        val self = this
        self.client.saveQueryToZeroSuggest(query)
    }

    private fun deleteQuery(query: String): Unit {
        val self = this
        self.client.deleteQueryFromZeroSuggest(query)
    }

    private fun handle(resolve: (Unit) -> Unit, reject: (YSError) -> Unit, error: YSError?, okMessage: String): Unit {
        if (error != null) {
            reject(error!!)
        } else {
            this.logger.info(okMessage)
            resolve(getVoid())
        }
    }

    private fun alll(promises: YSArray<XPromise<Unit>>): XPromise<Unit> {
        return all(promises).then( {
            _ ->
            getVoid()
        })
    }

    private fun getInboxIfTab(folderName: FolderName): FolderName {
        return if (isTab(folderName)) DefaultFolderName.inbox else folderName
    }

    private fun getRelevantIfTab(folderName: FolderName): FolderName {
        return if (isTab(folderName)) FolderBackendName.inbox else folderName
    }

    private fun isDefaultFolder(name: String): Boolean {
        val defaultNames = mutableListOf("INBOX", "INBOX/Social", "INBOX/Newsletters", "Отправленные", "Черновики", "Спам", "Корзина", "Yandex", "Drafts|template", DefaultFolderName.inbox, DefaultFolderName.trash, DefaultFolderName.draft, DefaultFolderName.template, DefaultFolderName.outgoing, DefaultFolderName.sent, DefaultFolderName.spam, DefaultFolderName.archive)
        return defaultNames.contains(name)
    }

}

