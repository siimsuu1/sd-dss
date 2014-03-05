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

package eu.europa.ec.markt.dss.ws.impl;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.jws.WebService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.ec.markt.dss.DSSUtils;
import eu.europa.ec.markt.dss.DigestAlgorithm;
import eu.europa.ec.markt.dss.exception.DSSException;
import eu.europa.ec.markt.dss.parameter.BLevelParameters;
import eu.europa.ec.markt.dss.parameter.SignatureParameters;
import eu.europa.ec.markt.dss.signature.DSSDocument;
import eu.europa.ec.markt.dss.signature.DocumentSignatureService;
import eu.europa.ec.markt.dss.signature.SignatureLevel;
import eu.europa.ec.markt.dss.signature.SignaturePackaging;
import eu.europa.ec.markt.dss.ws.SignatureService;
import eu.europa.ec.markt.dss.ws.WSDocument;
import eu.europa.ec.markt.dss.ws.WSParameters;

/**
 * Implementation of the Interface for the Contract of the Signature Web Service.
 *
 * @version $Revision: 3479 $ - $Date: 2014-02-19 11:50:33 +0100 (Wed, 19 Feb 2014) $
 */

@WebService(endpointInterface = "eu.europa.ec.markt.dss.ws.SignatureService", serviceName = "SignatureService")
public class SignatureServiceImpl implements SignatureService {

    private static final Logger LOG = LoggerFactory.getLogger(SignatureServiceImpl.class);

    private DocumentSignatureService xadesService;

    private DocumentSignatureService cadesService;

    private DocumentSignatureService padesService;

    private DocumentSignatureService asicService;

    /**
     * @param xadesService the xadesService to set
     */
    public void setXadesService(final DocumentSignatureService xadesService) {

        this.xadesService = xadesService;
    }

    /**
     * @param cadesService the cadesService to set
     */
    public void setCadesService(final DocumentSignatureService cadesService) {

        this.cadesService = cadesService;
    }

    /**
     * @param padesService the padesService to set
     */
    public void setPadesService(final DocumentSignatureService padesService) {

        this.padesService = padesService;
    }

    /**
     * @param asicService the asicService to set
     */
    public void setAsicService(final DocumentSignatureService asicService) {

        this.asicService = asicService;
    }

    private DocumentSignatureService getServiceForSignatureLevel(final SignatureLevel signatureLevel) {

        switch (signatureLevel) {
            case XAdES_BASELINE_B:
            case XAdES_BASELINE_T:
            case XAdES_C:
            case XAdES_X:
            case XAdES_XL:
            case XAdES_BASELINE_LT:
            case XAdES_A:
            case XAdES_BASELINE_LTA:
                return xadesService;
            case CAdES_BASELINE_B:
            case CAdES_BASELINE_T:
            case CAdES_BASELINE_LT:
            case CAdES_BASELINE_LTA:
                return cadesService;
            case PAdES_BASELINE_B:
            case PAdES_BASELINE_T:
            case PAdES_BASELINE_LT:
            case PAdES_BASELINE_LTA:
                return padesService;
            case ASiC_S_BASELINE_B:
            case ASiC_S_BASELINE_T:
            case ASiC_S_BASELINE_LT:
                return asicService;
            default:
                throw new IllegalArgumentException("Unrecognized format " + signatureLevel);
        }
    }

    private SignatureParameters createParameters(final WSParameters wsParameters) throws DSSException {

        if (wsParameters == null) {

            return null;
        }
        final SignatureParameters params = new SignatureParameters();

        setSignatureLevel(wsParameters, params);

        setSignaturePackaging(wsParameters, params);

        setDigestAlgorithm(wsParameters, params);

        setSigningDate(wsParameters, params);

        setSigningCertificateAndChain(wsParameters, params);

        setDeterministicId(wsParameters, params);

        setSignaturePolicy(wsParameters, params);

        setClaimedSignerRole(wsParameters, params);

        setContentIdentifierPrefix(wsParameters, params);
        setContentIdentifierSuffix(wsParameters, params);

        setCommitmentTypeIndication(wsParameters, params);

        setSignerLocation(wsParameters, params);

        return params;
    }

    private void setSignaturePolicy(WSParameters wsParameters, SignatureParameters params) {

        final BLevelParameters.Policy signaturePolicy = wsParameters.getSignaturePolicy();
        params.bLevel().setSignaturePolicy(signaturePolicy);
    }

    private void setSignerLocation(WSParameters wsParameters, SignatureParameters params) {

        final BLevelParameters.SignerLocation signerLocation = wsParameters.getSignerLocation();
        params.bLevel().setSignerLocation(signerLocation);
    }

    private void setCommitmentTypeIndication(WSParameters wsParameters, SignatureParameters params) {

        final List<String> commitmentTypeIndication = wsParameters.getCommitmentTypeIndication();
        params.bLevel().setCommitmentTypeIndications(commitmentTypeIndication);
    }

    private void setContentIdentifierSuffix(WSParameters wsParameters, SignatureParameters params) {

        final String contentIdentifierSuffix = wsParameters.getContentIdentifierSuffix();
        params.bLevel().setContentIdentifierSuffix(contentIdentifierSuffix);
    }

    private void setContentIdentifierPrefix(WSParameters wsParameters, SignatureParameters params) {

        final String contentIdentifierPrefix = wsParameters.getContentIdentifierPrefix();
        params.bLevel().setContentIdentifierPrefix(contentIdentifierPrefix);
    }

    private void setDigestAlgorithm(final WSParameters wsParameters, final SignatureParameters params) {

        final DigestAlgorithm digestAlgorithm = wsParameters.getDigestAlgorithm();
        params.setDigestAlgorithm(digestAlgorithm);
    }

    private void setDeterministicId(final WSParameters wsParameters, final SignatureParameters params) {

        final String deterministicId = wsParameters.getDeterministicId();
        params.setDeterministicId(deterministicId);
    }

    private void setClaimedSignerRole(final WSParameters wsParameters, final SignatureParameters params) {
        final List<String> claimedSignerRoles = wsParameters.getClaimedSignerRole();
        if (claimedSignerRoles != null) {
            for (final String claimedSignerRole : claimedSignerRoles) {

                params.bLevel().addClaimedSignerRole(claimedSignerRole);
            }
        }
    }

    private void setSigningCertificateAndChain(final WSParameters wsParameters, final SignatureParameters params) {

        final byte[] signingCertBytes = wsParameters.getSigningCertificateBytes();
        if (signingCertBytes == null) {
            return;
        }
        final X509Certificate x509SigningCertificate = DSSUtils.loadCertificate(signingCertBytes);
        params.setSigningCertificate(x509SigningCertificate);

        final List<X509Certificate> chain = new ArrayList<X509Certificate>();
        chain.add(x509SigningCertificate);
        final List<byte[]> certificateChainByteArrayList = wsParameters.getCertificateChainByteArrayList();
        if (certificateChainByteArrayList != null) {

            for (final byte[] x509CertificateBytes : certificateChainByteArrayList) {

                final X509Certificate x509Certificate = DSSUtils.loadCertificate(x509CertificateBytes);
                if (!chain.contains(x509Certificate)) {

                    chain.add(x509Certificate);
                }
            }
        }
        params.setCertificateChain(chain);
    }

    private void setSigningDate(final WSParameters wsParameters, final SignatureParameters params) {

        final Date signingDate = wsParameters.getSigningDate();
        params.bLevel().setSigningDate(signingDate);
    }

    private void setSignaturePackaging(final WSParameters wsParameters, final SignatureParameters params) {

        final SignaturePackaging signaturePackaging = wsParameters.getSignaturePackaging();
        params.setSignaturePackaging(signaturePackaging);
    }

    private void setSignatureLevel(WSParameters wsParameters, SignatureParameters params) {

        final SignatureLevel signatureLevel = wsParameters.getSignatureLevel();
        params.setSignatureLevel(signatureLevel);
    }

    @Override
    public byte[] getDataToSign(final WSDocument document, final WSParameters wsParameters) throws DSSException {

        String exceptionMessage;
        try {
            if (LOG.isInfoEnabled()) {

                LOG.info("WsGetDataToSign: begin");
            }
            final SignatureParameters params = createParameters(wsParameters);
            final DocumentSignatureService service = getServiceForSignatureLevel(params.getSignatureLevel());
            final byte[] dataToSign = service.getDataToSign(document, params);
            if (LOG.isInfoEnabled()) {

                LOG.info("WsGetDataToSign: end");
            }
            return dataToSign;
        } catch (Throwable e) {
            e.printStackTrace();
            exceptionMessage = e.getMessage();
        }
        LOG.info("WsGetDataToSign: end with exception");
        throw new DSSException(exceptionMessage);
    }

    @Override
    public WSDocument signDocument(final WSDocument document, final WSParameters wsParameters, final byte[] signatureValue) throws DSSException {

        String exceptionMessage;
        try {
            if (LOG.isInfoEnabled()) {

                LOG.info("WsSignDocument: begin");
            }
            final SignatureParameters params = createParameters(wsParameters);
            final DocumentSignatureService service = getServiceForSignatureLevel(params.getSignatureLevel());

            final DSSDocument dssDocument = service.signDocument(document, params, signatureValue);
            WSDocument wsDocument = new WSDocument(dssDocument);
            if (LOG.isInfoEnabled()) {

                LOG.info("WsSignDocument: end");
            }
            return wsDocument;
        } catch (Throwable e) {
            e.printStackTrace();
            exceptionMessage = e.getMessage();
        }
        LOG.info("WsSignDocument: end with exception");
        throw new DSSException(exceptionMessage);
    }

    @Override
    public WSDocument extendSignature(final WSDocument signedDocument, final WSParameters wsParameters) throws DSSException {

        String exceptionMessage;
        try {
            if (LOG.isInfoEnabled()) {

                LOG.info("WsExtendSignature: begin");
            }
            final SignatureParameters params = createParameters(wsParameters);
            final DocumentSignatureService service = getServiceForSignatureLevel(params.getSignatureLevel());
            final DSSDocument dssDocument = service.extendDocument(signedDocument, params);
            final WSDocument wsDocument = new WSDocument(dssDocument);
            if (LOG.isInfoEnabled()) {

                LOG.info("WsExtendSignature: end");
            }
            return wsDocument;
        } catch (Throwable e) {
            e.printStackTrace();
            exceptionMessage = e.getMessage();
        }
        LOG.info("WsExtendSignature: end with exception");
        throw new DSSException(exceptionMessage);
    }
}