/*
 * DSS - Digital Signature Services
 *
 * Copyright (C) 2013 European Commission, Directorate-General Internal Market and Services (DG MARKT), B-1049 Bruxelles/Brussel
 *
 * Developed by: 2013 ARHS Developments S.A. (rue Nicolas Bové 2B, L-1253 Luxembourg) http://www.arhs-developments.com
 *
 * This file is part of the "DSS - Digital Signature Services" project.
 *
 * "DSS - Digital Signature Services" is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * DSS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * "DSS - Digital Signature Services".  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Project: Digital Signature Services (DSS)
 * Contractor: ARHS-Developments
 *
 * $HeadURL: http://forge.aris-lux.lan/svn/dgmarktdss/trunk/apps/dss/core/dss-spi/src/main/java/eu/europa/ec/markt/dss/signature/MimeType.java $
 * $Revision: 3436 $
 * $Date: 2014-02-11 13:30:17 +0100 (Tue, 11 Feb 2014) $
 * $Author: bielecro $
 */
package eu.europa.ec.markt.dss.signature;

/**
 * TODO
 *
 * <p> DISCLAIMER: Project owner DG-MARKT.
 *
 * @author <a href="mailto:dgmarkt.Project-DSS@arhs-developments.com">ARHS Developments</a>
 * @version $Revision: 3436 $ - $Date: 2014-02-11 13:30:17 +0100 (Tue, 11 Feb 2014) $
 */
public enum MimeType {

    BINARY("application/octet-stream"), XML("text/xml"), PDF("application/pdf"), PKCS7("application/pkcs7-signature"), ASICS("application/vnd.etsi.asic-s+zip"), TEXT("text/plain");

    private String code;

    /**
     * The default constructor for MimeTypes.
     */
    private MimeType(final String code) {
        this.code = code;
    }

    /**
     * @return the code
     */
    public String getCode() {
        return code;
    }

    public static MimeType fromFileName(final String name) {
        final String lowerCaseName = name.toLowerCase();
        if (lowerCaseName.endsWith(".xml")) {
            return XML;
        } else if (lowerCaseName.endsWith(".pdf")) {
            return PDF;
        } else if (lowerCaseName.endsWith(".asics") || lowerCaseName.endsWith(".scs")) {
            //// TODO: (Bob: 2014 Jan 28) To include zip as asic the check must be performed more deeply
            return ASICS;
        } else if (lowerCaseName.endsWith(".txt")) {
            return TEXT;
        } else {
            return BINARY;
        }
    }

    public static MimeType fromCode(final String mimeTypeString) {

        for (final MimeType mimeType : values()) {

            if (mimeType.code.equals(mimeTypeString)) {
                return mimeType;
            }
        }
        return null;
    }
}
