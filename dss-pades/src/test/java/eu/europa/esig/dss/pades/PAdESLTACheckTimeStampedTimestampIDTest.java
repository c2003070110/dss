package eu.europa.esig.dss.pades;

import java.io.File;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import eu.europa.esig.dss.DSSDocument;
import eu.europa.esig.dss.FileDocument;
import eu.europa.esig.dss.SignatureAlgorithm;
import eu.europa.esig.dss.SignatureLevel;
import eu.europa.esig.dss.SignatureValue;
import eu.europa.esig.dss.ToBeSigned;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.test.TestUtils;
import eu.europa.esig.dss.test.gen.CertificateService;
import eu.europa.esig.dss.test.mock.MockPrivateKeyEntry;
import eu.europa.esig.dss.test.mock.MockTSPSource;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.reports.Reports;
import eu.europa.esig.dss.validation.reports.wrapper.DiagnosticData;
import eu.europa.esig.dss.validation.reports.wrapper.TimestampWrapper;
import eu.europa.esig.dss.x509.TimestampType;

public class PAdESLTACheckTimeStampedTimestampIDTest {

	@Test
	public void test() throws Exception {
		DSSDocument documentToSign = new FileDocument(new File("src/test/resources/sample.pdf"));

		CertificateService certificateService = new CertificateService();
		MockPrivateKeyEntry privateKeyEntry = certificateService.generateCertificateChain(SignatureAlgorithm.RSA_SHA256);

		PAdESSignatureParameters signatureParameters = new PAdESSignatureParameters();
		signatureParameters.bLevel().setSigningDate(new Date());
		signatureParameters.setSigningCertificate(privateKeyEntry.getCertificate());
		signatureParameters.setCertificateChain(privateKeyEntry.getCertificateChain());
		signatureParameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_LTA);

		CertificateVerifier certificateVerifier = new CommonCertificateVerifier();
		PAdESService service = new PAdESService(certificateVerifier);
		service.setTspSource(new MockTSPSource(certificateService.generateTspCertificate(SignatureAlgorithm.RSA_SHA1)));

		ToBeSigned toBeSigned = service.getDataToSign(documentToSign, signatureParameters);
		SignatureValue signatureValue = TestUtils.sign(signatureParameters.getSignatureAlgorithm(), privateKeyEntry, toBeSigned);
		final DSSDocument signedDocument = service.signDocument(documentToSign, signatureParameters, signatureValue);

		SignedDocumentValidator validator = SignedDocumentValidator.fromDocument(signedDocument);
		validator.setCertificateVerifier(new CommonCertificateVerifier());

		Reports report = validator.validateDocument();
		// report.print();
		DiagnosticData diagnostic = report.getDiagnosticData();
		String timestampId = diagnostic.getSignatures().get(0).getTimestampList().get(0).getId();
		for (TimestampWrapper wrapper : diagnostic.getTimestampList(diagnostic.getFirstSignatureId())) {
			if (wrapper.getType().equals(TimestampType.ARCHIVE_TIMESTAMP.toString())) {
				Assert.assertEquals(timestampId, wrapper.getSignedObjects().getTimestampedTimestamp().get(0).getId());
			}
		}
	}
}
