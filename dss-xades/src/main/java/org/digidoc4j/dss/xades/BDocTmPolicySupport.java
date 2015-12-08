/**
 * DSS - Digital Signature Services
 * Copyright (C) 2015 European Commission, provided under the CEF programme
 *
 * This file is part of the "DSS - Digital Signature Services" project.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.digidoc4j.dss.xades;

import eu.europa.esig.dss.Policy;
import eu.europa.esig.dss.xades.XAdESSignatureParameters;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

/**
 * Support for BDoc TM profile signatures
 */
public class BDocTmPolicySupport implements Serializable {

    public static final String BDOC_TM_POLICY_ID = "urn:oid:1.3.6.1.4.1.10015.1000.3.2.1";
    public static final String BDOC_TM_POLICY_QUALIFIER = "OIDAsURN";

    public static boolean isBdocTmSignatureProfile(XAdESSignatureParameters params) {
        Policy signaturePolicy = params.bLevel().getSignaturePolicy();
        if(signaturePolicy == null) {
            return false;
        }
        String policyId = StringUtils.trim(signaturePolicy.getId());
        return BDOC_TM_POLICY_ID.equals(policyId);
    }
}
