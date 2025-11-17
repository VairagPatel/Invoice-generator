import { useContext, useEffect, useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import InvoicePreview from "./InvoicePreview";
import toast from "react-hot-toast";
import { Loader2 } from "lucide-react";
import { AppContext } from "../context/AppContext";
import { templates } from "../assets/assets";
import { generatePdfFromElement } from "../utils/pdfUtils";
import { uploadInvoiceThumbnail } from "../service/cloudinaryService";
import {
  deleteInvoice,
  saveInvoice,
  sendInvoice,
} from "../service/invoiceService";
import html2canvas from "html2canvas";
import { useAuth, useUser } from "@clerk/clerk-react";

const PreviewPage = () => {
  const { invoiceData, selectedTemplate, setSelectedTemplate, baseURL } =
    useContext(AppContext);
  const { user } = useUser();

  const { getToken } = useAuth();

  const navigate = useNavigate();
  const previewRef = useRef();
  const [loading, setLoading] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [customerEmail, setCustomerEmail] = useState("");
  const [emailing, setEmailing] = useState(false);

  useEffect(() => {
    if (!invoiceData || !invoiceData.items?.length) {
      toast.error("Invoice data is missing.");
      navigate("/");
    }
  }, [invoiceData, navigate]);

  const handleDownloadPdf = async () => {
    if (!previewRef.current) return;

    try {
      setDownloading(true);
      await generatePdfFromElement(
        previewRef.current,
        `invoice_${Date.now()}.pdf`
      );
    } catch (error) {
      toast.error("Failed to download PDF.");
      console.error(error);
    } finally {
      setDownloading(false);
    }
  };

  const handleSaveAndExit = async () => {
    if (!previewRef.current) return toast.error("Preview not available");

    try {
      setLoading(true);

      const canvas = await html2canvas(previewRef.current, {
        scale: 2,
        useCORS: true,
        backgroundColor: "#fff",
        scrollY: -window.scrollY,
      });

      const imageData = canvas.toDataURL("image/png");
      const thumbnailUrl = await uploadInvoiceThumbnail(imageData);

      const payload = {
        ...invoiceData,
        clerkId: user.id,
        thumbnailUrl,
        template: selectedTemplate,
      };
      const token = await getToken();
      const response = await saveInvoice(baseURL, payload, token);

      if (response.status === 200) {
        toast.success("Invoice saved successfully!");
        navigate("/dashboard");
      } else {
        throw new Error("Save failed");
      }
    } catch (error) {
      console.error(error);
      toast.error("Failed to save invoice");
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!invoiceData.id) return toast.error("No invoice ID found.");

    try {
      const token = await getToken();
      const res = await deleteInvoice(baseURL, invoiceData.id, token);
      if (res.status === 204) {
        toast.success("Invoice deleted.");
        navigate("/dashboard");
      } else {
        toast.error("Unable to delete invoice.");
      }
    } catch (err) {
      toast.error("Delete failed.");
      console.error(err);
    }
  };

  const handleSendEmail = async () => {
    if (!previewRef.current || !customerEmail) {
      return toast.error("Please enter a valid email and try again.");
    }

    try {
      setEmailing(true);

      // Generate the PDF blob
      const pdfBlob = await generatePdfFromElement(
        previewRef.current,
        `invoice_${Date.now()}.pdf`,
        true
      ); // add `returnBlob=true` in your utils

      const formData = new FormData();
      formData.append("file", pdfBlob, `invoice_${Date.now()}.pdf`);
      formData.append("email", customerEmail);

      const token = await getToken();

      const response = await sendInvoice(baseURL, token, formData);

      if (response.status === 200) {
        toast.success("Email sent successfully!");
        setShowModal(false);
        setCustomerEmail("");
      } else {
        toast.error("Failed to send email.");
      }
    } catch (error) {
      console.error(error);
      toast.error("Something went wrong while sending email.");
    } finally {
      setEmailing(false);
    }
  };

  return (
    <div
      className="container-fluid d-flex flex-column p-3"
      style={{ minHeight: "100vh" }}
    >
      <div className="d-flex flex-column align-items-center mb-4 gap-3">
        <div className="d-flex gap-2 flex-wrap justify-content-center">
          {templates.map(({ id, label }) => (
            <button
              key={id}
              onClick={() => setSelectedTemplate(id)}
              className={`btn btn-sm rounded-pill p-2 ${
                selectedTemplate === id
                  ? "btn-warning"
                  : "btn-outline-secondary"
              }`}
              style={{ height: "38px", minWidth: "100px" }}
            >
              {label}
            </button>
          ))}
        </div>

        <div className="d-flex flex-wrap justify-content-center gap-2">
          <button
            className="btn btn-primary d-flex align-items-center justify-content-center"
            onClick={handleSaveAndExit}
            disabled={loading}
          >
            {loading && <Loader2 className="me-2 spin-animation" size={18} />}
            {loading ? "Saving..." : "Save and Exit"}
          </button>
          <button className="btn btn-danger" onClick={handleDelete}>
            Delete invoice
          </button>
          <button className="btn btn-secondary" onClick={() => navigate("/")}>
            Back to Dashboard
          </button>
          <button className="btn btn-info" onClick={() => setShowModal(true)}>
            Send Email
          </button>
          <button
            className="btn btn-success d-flex align-items-center justify-content-center"
            onClick={handleDownloadPdf}
            disabled={downloading}
          >
            {downloading && (
              <Loader2 className="me-2 spin-animation" size={18} />
            )}
            {downloading ? "Downloading..." : "Download PDF"}
          </button>
        </div>
      </div>

      <div className="flex-grow-1 overflow-auto d-flex justify-content-center align-items-start bg-light py-3">
        <div
          ref={previewRef}
          style={{
            width: "794px",
            minHeight: "1123px",
            backgroundColor: "#fff",
            boxShadow: "0 0 8px rgba(0,0,0,0.15)",
            margin: 0,
            padding: 0,
            overflow: "hidden",
          }}
        >
          <InvoicePreview
            invoiceData={invoiceData}
            template={selectedTemplate}
          />
        </div>
      </div>
      {showModal && (
        <div
          className="modal d-block"
          tabIndex="-1"
          role="dialog"
          style={{ backgroundColor: "rgba(0,0,0,0.5)" }}
        >
          <div className="modal-dialog" role="document">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">Send Invoice</h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setShowModal(false)}
                ></button>
              </div>
              <div className="modal-body">
                <input
                  type="email"
                  className="form-control"
                  placeholder="Customer Email"
                  value={customerEmail}
                  onChange={(e) => setCustomerEmail(e.target.value)}
                />
              </div>
              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={handleSendEmail}
                  disabled={emailing}
                >
                  {emailing ? "Sending..." : "Send"}
                </button>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setShowModal(false)}
                >
                  Cancel
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PreviewPage;
