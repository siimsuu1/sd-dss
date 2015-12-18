package eu.europa.esig.dss.EN319102.bbb.xcv;

import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import eu.europa.esig.dss.EN319102.bbb.AbstractBasicBuildingBlock;
import eu.europa.esig.dss.EN319102.bbb.ChainItem;
import eu.europa.esig.dss.EN319102.bbb.xcv.checks.CertificateCryptographicCheck;
import eu.europa.esig.dss.EN319102.bbb.xcv.checks.CertificateExpirationCheck;
import eu.europa.esig.dss.EN319102.bbb.xcv.checks.CertificateSignatureValidCheck;
import eu.europa.esig.dss.EN319102.bbb.xcv.checks.IntermediateCertificateRevoked;
import eu.europa.esig.dss.EN319102.bbb.xcv.checks.KeyUsageCheck;
import eu.europa.esig.dss.EN319102.bbb.xcv.checks.ProspectiveCertificateChainCheck;
import eu.europa.esig.dss.EN319102.bbb.xcv.checks.RevocationDataAvailableCheck;
import eu.europa.esig.dss.EN319102.bbb.xcv.checks.RevocationDataTrustedCheck;
import eu.europa.esig.dss.EN319102.bbb.xcv.checks.RevocationFreshnessCheck;
import eu.europa.esig.dss.EN319102.bbb.xcv.checks.SigningCertificateIssuedToLegalPersonCheck;
import eu.europa.esig.dss.EN319102.bbb.xcv.checks.SigningCertificateOnHoldCheck;
import eu.europa.esig.dss.EN319102.bbb.xcv.checks.SigningCertificateQualifiedCheck;
import eu.europa.esig.dss.EN319102.bbb.xcv.checks.SigningCertificateRevokedCheck;
import eu.europa.esig.dss.EN319102.bbb.xcv.checks.SigningCertificateSupportedBySSCDCheck;
import eu.europa.esig.dss.EN319102.bbb.xcv.checks.SigningCertificateTSLStatusAndValidityCheck;
import eu.europa.esig.dss.EN319102.bbb.xcv.checks.SigningCertificateTSLStatusCheck;
import eu.europa.esig.dss.EN319102.bbb.xcv.checks.SigningCertificateTSLValidityCheck;
import eu.europa.esig.dss.jaxb.detailedreport.XmlXCV;
import eu.europa.esig.dss.jaxb.diagnostic.XmlChainCertificate;
import eu.europa.esig.dss.validation.CertificateWrapper;
import eu.europa.esig.dss.validation.TokenProxy;
import eu.europa.esig.dss.validation.policy.ValidationPolicy2;
import eu.europa.esig.dss.validation.policy.ValidationPolicy2.Context;
import eu.europa.esig.dss.validation.policy.ValidationPolicy2.SubContext;
import eu.europa.esig.dss.validation.report.DiagnosticData;
import eu.europa.esig.jaxb.policy.CryptographicConstraint;
import eu.europa.esig.jaxb.policy.LevelConstraint;
import eu.europa.esig.jaxb.policy.MultiValuesConstraint;
import eu.europa.esig.jaxb.policy.RevocationConstraints;

/**
 * 5.2.6 X.509 certificate validation
 * This building block validates the signing certificate at current time.
 */
public class X509CertificateValidation extends AbstractBasicBuildingBlock<XmlXCV> {

	private final DiagnosticData diagnosticData;
	private final CertificateWrapper currentCertificate;
	private final Date currentTime;

	private final Context context;
	private final ValidationPolicy2 validationPolicy;

	private ChainItem<XmlXCV> firstItem;
	private XmlXCV result = new XmlXCV();

	public X509CertificateValidation(DiagnosticData diagnosticData, CertificateWrapper currentCertificate, Date currentTime, Context context, ValidationPolicy2 validationPolicy) {
		this.diagnosticData = diagnosticData;
		this.currentCertificate = currentCertificate;
		this.currentTime = currentTime;

		this.context = context;
		this.validationPolicy = validationPolicy;
	}

	@Override
	public void initChain() {
		ChainItem<XmlXCV> item = firstItem = certificateExpiration(currentCertificate, SubContext.SIGNING_CERT);
		item = item.setNextItem(prospectiveCertificateChain());

		List<XmlChainCertificate> certificateChainList = currentCertificate.getCertificateChain();

		if (CollectionUtils.isNotEmpty(certificateChainList)) {
			for (XmlChainCertificate chainCertificate : certificateChainList) {
				CertificateWrapper certificate = diagnosticData.getUsedCertificateByIdNullSafe(chainCertificate.getId());

				SubContext currentSubContext = SubContext.SIGNING_CERT;

				if (!StringUtils.equals(currentCertificate.getId(), certificate.getId())) { // CA Certificate
					currentSubContext = SubContext.CA_CERTIFICATE;
					item = item.setNextItem(certificateExpiration(certificate, currentSubContext));
				}

				item = item.setNextItem(keyUsage(certificate, currentSubContext));

				item = item.setNextItem(certificateSignatureValid(certificate, currentSubContext));

				item = item.setNextItem(revocationDataAvailable(certificate, currentSubContext));

				item = item.setNextItem(revocationDataTrusted(certificate, currentSubContext));

				item = item.setNextItem(revocationFreshness(certificate));

				if (SubContext.SIGNING_CERT.equals(currentSubContext)) { // TODO Why ??

					item = item.setNextItem(signingCertificateRevoked(certificate, currentSubContext));

					item = item.setNextItem(signingCertificateOnHold(certificate, currentSubContext));

					item = item.setNextItem(signingCertificateInTSLValidity(certificate));

					item = item.setNextItem(signingCertificateTSLStatus(certificate));

					item = item.setNextItem(signingCertificateTSLStatusAndValidity(certificate));

				} else {

					item = item.setNextItem(intermediateCertificateRevoked(certificate, currentSubContext));

				}

				// check cryptographic constraints for the revocation token
				item = item.setNextItem(certificateCryptographic(certificate.getRevocationData(), Context.REVOCATION, currentSubContext));

			}
		}

		// These constraints apply only to the main signature
		if (Context.MAIN_SIGNATURE.equals(context)) {
			item = item.setNextItem(signingCertificateQualified(currentCertificate));

			item = item.setNextItem(signingCertificateSupportedBySSCD(currentCertificate));

			item = item.setNextItem(signingCertificateIssuedToLegalPerson(currentCertificate));
		}

		if (CollectionUtils.isNotEmpty(certificateChainList)) {
			String lastChainCertId = currentCertificate.getLastChainCertificateId();
			for (XmlChainCertificate chainCertificate : certificateChainList) {
				CertificateWrapper chainItem = diagnosticData.getUsedCertificateByIdNullSafe(chainCertificate.getId());

				/**
				 * The trusted anchor is not checked. In the case of a certificate chain consisting of a single certificate
				 * which is trusted we need to set this variable to true.
				 */
				if (StringUtils.equals(lastChainCertId, chainCertificate.getId()) &&  chainItem.isTrusted()){
					continue;
				}

				SubContext currentSubContext = StringUtils.equals(chainItem.getId(), currentCertificate.getId()) ? SubContext.SIGNING_CERT : SubContext.CA_CERTIFICATE;
				item = item.setNextItem(certificateCryptographic(chainItem, context, currentSubContext));
			}
		}
	}

	private ChainItem<XmlXCV> prospectiveCertificateChain() {
		LevelConstraint constraint = validationPolicy.getProspectiveCertificateChainConstraint(context);
		return new ProspectiveCertificateChainCheck(result, currentCertificate, diagnosticData, constraint);
	}

	private ChainItem<XmlXCV> certificateExpiration(CertificateWrapper certificate, SubContext subContext) {
		LevelConstraint constraint = validationPolicy.getSigningCertificateExpirationConstraint(context, subContext);
		return new CertificateExpirationCheck(result, certificate, currentTime, constraint);
	}

	private ChainItem<XmlXCV> keyUsage(CertificateWrapper certificate, SubContext subContext) {
		// TODO multi context
		MultiValuesConstraint constraint = validationPolicy.getSigningCertificateKeyUsageConstraint(context);
		return new KeyUsageCheck(result, certificate, constraint);
	}

	private ChainItem<XmlXCV> certificateSignatureValid(CertificateWrapper certificate, SubContext subContext) {
		LevelConstraint constraint = validationPolicy.getCertificateSignatureConstraint(context, subContext);
		return new CertificateSignatureValidCheck(result, certificate, constraint);
	}

	private ChainItem<XmlXCV> revocationDataAvailable(CertificateWrapper certificate, SubContext subContext) {
		LevelConstraint constraint = validationPolicy.getRevocationDataAvailableConstraint(context, subContext);
		return new RevocationDataAvailableCheck(result, certificate, constraint);
	}

	private ChainItem<XmlXCV> revocationDataTrusted(CertificateWrapper certificate, SubContext subContext) {
		LevelConstraint constraint = validationPolicy.getRevocationDataIsTrustedConstraint(context, subContext);
		return new RevocationDataTrustedCheck(result, certificate, constraint);
	}

	private ChainItem<XmlXCV> revocationFreshness(CertificateWrapper certificate) {
		RevocationConstraints revocationConstraints = validationPolicy.getRevocationConstraint();
		return new RevocationFreshnessCheck(result, certificate, currentTime, revocationConstraints);
	}

	private ChainItem<XmlXCV> signingCertificateRevoked(CertificateWrapper certificate, SubContext subContext) {
		LevelConstraint constraint = validationPolicy.getSigningCertificateRevokedConstraint(context, subContext);
		return new SigningCertificateRevokedCheck(result, certificate, constraint);
	}

	private ChainItem<XmlXCV> intermediateCertificateRevoked(CertificateWrapper certificate, SubContext subContext) {
		LevelConstraint constraint = validationPolicy.getSigningCertificateRevokedConstraint(context, subContext);
		return new IntermediateCertificateRevoked(result, certificate, constraint);
	}

	private ChainItem<XmlXCV> signingCertificateOnHold(CertificateWrapper certificate, SubContext subContext) {
		LevelConstraint constraint = validationPolicy.getSigningCertificateOnHoldConstraint(context, subContext);
		return new SigningCertificateOnHoldCheck(result, certificate, constraint);
	}

	private ChainItem<XmlXCV> signingCertificateInTSLValidity(CertificateWrapper certificate) {
		LevelConstraint constraint = validationPolicy.getSigningCertificateTSLValidityConstraint(context);
		return new SigningCertificateTSLValidityCheck(result, certificate, constraint);
	}

	private ChainItem<XmlXCV> signingCertificateTSLStatus(CertificateWrapper certificate) {
		LevelConstraint constraint = validationPolicy.getSigningCertificateTSLStatusConstraint(context);
		return new SigningCertificateTSLStatusCheck(result, certificate, constraint);
	}

	private ChainItem<XmlXCV> signingCertificateTSLStatusAndValidity(CertificateWrapper certificate) {
		LevelConstraint constraint = validationPolicy.getSigningCertificateTSLStatusAndValidityConstraint(context);
		return new SigningCertificateTSLStatusAndValidityCheck(result, certificate, constraint);
	}

	private ChainItem<XmlXCV> certificateCryptographic(TokenProxy token, Context context, SubContext subcontext) {
		CryptographicConstraint cryptographicConstraint = validationPolicy.getSignatureCryptographicConstraint(context, subcontext);
		return new CertificateCryptographicCheck(result, token, currentTime, cryptographicConstraint);
	}

	private ChainItem<XmlXCV> signingCertificateQualified(CertificateWrapper certificate) {
		LevelConstraint constraint = validationPolicy.getSigningCertificateQualificationConstraint();
		return new SigningCertificateQualifiedCheck(result, certificate, constraint);
	}

	private ChainItem<XmlXCV> signingCertificateSupportedBySSCD(CertificateWrapper certificate) {
		LevelConstraint constraint = validationPolicy.getSigningCertificateSupportedBySSCDConstraint();
		return new SigningCertificateSupportedBySSCDCheck(result, certificate, constraint);
	}

	private ChainItem<XmlXCV> signingCertificateIssuedToLegalPerson(CertificateWrapper certificate) {
		LevelConstraint constraint = validationPolicy.getSigningCertificateIssuedToLegalPersonConstraint();
		return new SigningCertificateIssuedToLegalPersonCheck(result, certificate, constraint);
	}

	@Override
	public XmlXCV execute() {
		firstItem.execute();
		return result;
	}

}
