package main

import (
	"context"
	"crypto/tls"
	"crypto/x509"
	"io/ioutil"
	"log"
	"net/http"
	"time"

	cloudevents "github.com/cloudevents/sdk-go"
	"github.com/google/uuid"
)

func main() {
	event := cloudevents.NewEvent()
	event.Context = cloudevents.EventContextV03{}.AsV03()
	event.SetID(uuid.New().String())
	event.SetType("order.created")
	event.SetSource("external-application")
	event.SetTime(time.Now())
	event.SetExtension("eventtypeversion", "v1")
	event.SetDataContentType("application/json")
	event.SetData(23)

	// Add path to client certificate and key
	cert, err := tls.LoadX509KeyPair("generated.crt", "generated.key")
	if err != nil {
		log.Fatalln("Unable to load cert", err)
	}
	clientCACert, err := ioutil.ReadFile("generated.crt")
	if err != nil {
		log.Fatal("Unable to open cert", err)
	}

	clientCertPool := x509.NewCertPool()
	clientCertPool.AppendCertsFromPEM(clientCACert)

	tlsConfig := &tls.Config{
		Certificates:       []tls.Certificate{cert},
		RootCAs:            clientCertPool,
		InsecureSkipVerify: true,
	}

	tlsConfig.BuildNameToCertificate()

	client := &http.Client{
		Transport: &http.Transport{TLSClientConfig: tlsConfig},
	}
	t, err := cloudevents.NewHTTPTransport(
		cloudevents.WithTarget("https://gateway.kyma.local/sample-external-solution/v2/events"),
		cloudevents.WithStructuredEncoding())

	t.Client = client
	c, err := cloudevents.NewClient(t)
	if err != nil {
		panic("unable to create cloudevent client: " + err.Error())
	}
	_, err = c.Send(context.Background(), event)
	if err != nil {
		panic("failed to send cloudevent: " + err.Error())
	}
}