/*
 * Copyright 2010-2013 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.invoice.template;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import org.killbill.billing.account.api.Account;
import org.killbill.billing.currency.api.CurrencyConversionApi;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.formatters.InvoiceFormatter;
import org.killbill.billing.invoice.api.formatters.InvoiceFormatterFactory;
import org.killbill.billing.invoice.template.translator.DefaultInvoiceTranslator;
import org.killbill.billing.util.LocaleUtils;
import org.killbill.billing.util.email.templates.TemplateEngine;
import org.killbill.billing.util.template.translation.TranslatorConfig;

import com.google.common.base.Strings;
import com.google.inject.Inject;

public class HtmlInvoiceGenerator {

    private final InvoiceFormatterFactory factory;
    private final TemplateEngine templateEngine;
    private final TranslatorConfig config;
    private final CurrencyConversionApi currencyConversionApi;

    @Inject
    public HtmlInvoiceGenerator(final InvoiceFormatterFactory factory, final TemplateEngine templateEngine,
                                final TranslatorConfig config, final CurrencyConversionApi currencyConversionApi) {
        this.factory = factory;
        this.templateEngine = templateEngine;
        this.config = config;
        this.currencyConversionApi = currencyConversionApi;
    }

    public HtmlInvoice generateInvoice(final Account account, @Nullable final Invoice invoice, final boolean manualPay) throws IOException {
        // Don't do anything if the invoice is null
        if (invoice == null) {
            return null;
        }

        HtmlInvoice invoiceData = new HtmlInvoice();
        final Map<String, Object> data = new HashMap<String, Object>();
        final DefaultInvoiceTranslator invoiceTranslator = new DefaultInvoiceTranslator(config);
        final String accountLocale = Strings.emptyToNull(account.getLocale());
        // If no Locale is defined, use the default JVM one
        final Locale locale = accountLocale == null ? Locale.getDefault() : LocaleUtils.toLocale(accountLocale);
        invoiceTranslator.setLocale(locale);
        data.put("text", invoiceTranslator);
        data.put("account", account);

        final InvoiceFormatter formattedInvoice = factory.createInvoiceFormatter(config, invoice, locale, currencyConversionApi);
        data.put("invoice", formattedInvoice);

        invoiceData.setSubject(invoiceTranslator.getInvoiceEmailSubject());

        if (manualPay) {
            invoiceData.setBody(templateEngine.executeTemplate(config.getManualPayTemplateName(), data));
        } else {
            invoiceData.setBody(templateEngine.executeTemplate(config.getTemplateName(), data));
        }

        return invoiceData;
    }
}
