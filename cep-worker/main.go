package main

import (
	"fmt"
	"time"
	"math/rand"
	"strconv"
  //  "strings"


	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"

	"k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	apiv1 "k8s.io/api/core/v1"
	resource "k8s.io/apimachinery/pkg/api/resource"
	"flag"
	"os"

)

func main() {

	arr := os.Args

	fmt.Println("begin")

	fmt.Println(arr)
	
	if( len(arr) > 1 ) {
		if( arr[1] == "create" ) {
			create(arr[2])
		} else if( arr[1] == "delete" ) {
			del(arr[2])
		} else if( arr[1] == "test" ) {
			fmt.Printf("calling test")
			test()
		}
	} else {
		
		list()
	}
	
}

func create(workerid string) bool{

	// creates the in-cluster config
	config, err := rest.InClusterConfig()
	if err != nil {
		panic(err.Error())
	}
	//fmt.Printf("cluster config %q.\n", config)
	// creates the clientset
	clientset, err := kubernetes.NewForConfig(config)
	if err != nil {
		panic(err.Error())
	}

    //fmt.Printf("clientset config %q.\n", clientset)
	flag.Parse()

	podsClient := clientset.CoreV1().Pods(apiv1.NamespaceDefault)

	//fmt.Printf("pods client config %q.\n", podsClient)

	cep_worker_image := os.Getenv("IMAGE_TAG")
	mem_res := os.Getenv("REQUEST_MEMORY")
	mem_lim := os.Getenv("LIMIT_MEMORY")

	value_ASAT := true

	redispod := &apiv1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name: "worker"+workerid ,
			Namespace : "default",
		},
		Spec: apiv1.PodSpec{
			Containers: []apiv1.Container{
				{
					Name: "worker"+workerid,
					Image: "fernandofreire/cep-worker:"+cep_worker_image ,
					Env: []apiv1.EnvVar{
						{
                	    	Name: "WORKER_ID",
                        	Value: workerid	,
						},
						{
                	    	Name: "IMAGE_TAG",
                        	Value: cep_worker_image	,
						},
					},
					Resources : apiv1.ResourceRequirements{
						Requests: apiv1.ResourceList{
							"memory": resource.MustParse(mem_res+"Mi"),
						},
      					Limits: apiv1.ResourceList{
							"memory": resource.MustParse(mem_lim+"Mi"),
						},
					},
				},
			},
			ServiceAccountName: "cep-handler-sa",
			AutomountServiceAccountToken: &value_ASAT,
		},
	}
    //"cpu" : resource.MustParse("500m"),
    //"cpu": resource.MustParse("500m"),

	fmt.Println("Creating cep-worker pod...")
	redisresult, err := podsClient.Create(redispod)
	if err != nil {
	    fmt.Printf("Not Created pod %q.\n", redisresult)
		panic(err)
		return false
	}
	fmt.Printf("Created pod %q.\n", redisresult.GetObjectMeta().GetName())
	return true
}

func del(workerid string) bool {

	// creates the in-cluster config
	config, err := rest.InClusterConfig()
	if err != nil {
		panic(err.Error())
	}
	// creates the clientset
	clientset, err := kubernetes.NewForConfig(config)
	if err != nil {
		panic(err.Error())
	}


	flag.Parse()

	podsClient := clientset.CoreV1().Pods(apiv1.NamespaceDefault)

	fmt.Println("Deleting ...")
	deletePolicy := metav1.DeletePropagationForeground
	if err := podsClient.Delete("worker"+workerid , &metav1.DeleteOptions{
		PropagationPolicy: &deletePolicy,
	}); err != nil {
		panic(err)
		return false
	}
	return true
}

func test() bool {

	fmt.Println("test funtion started")

	rand.Seed(time.Now().UnixNano())

	for {

		num := rand.Intn(10000)
		
        fmt.Println("output generated")

		time.Sleep(30 * time.Second)
		
		create(strconv.Itoa(num))
	}
	return true
}

func list(){

	// creates the in-cluster config
	config, err := rest.InClusterConfig()
	if err != nil {
		panic(err.Error())
	}
	// creates the clientset
	clientset, err := kubernetes.NewForConfig(config)
	if err != nil {
		panic(err.Error())
	}
	for {
		// get pods in all the namespaces by omitting namespace
		// Or specify namespace to get pods in particular namespace
		pods, err := clientset.CoreV1().Pods("default").List(metav1.ListOptions{ })
		if err != nil {
			panic(err.Error())
		}
		fmt.Printf("There are %d pods in the default namespace\n", len(pods.Items))

		// Examples for error handling:
		// - Use helper functions e.g. errors.IsNotFound()
		// - And/or cast to StatusError and use its properties like e.g. ErrStatus.Message

		_, err = clientset.CoreV1().Pods("default").Get("example-xxxxx", metav1.GetOptions{})
		if errors.IsNotFound(err) {
			fmt.Printf("Pod example-xxxxx not found in default namespace\n")
		} else if statusError, isStatus := err.(*errors.StatusError); isStatus {
			fmt.Printf("Error getting pod %v\n", statusError.ErrStatus.Message)
		} else if err != nil {
			panic(err.Error())
		} else {
			fmt.Printf("Found example-xxxxx pod in default namespace\n")
		}

		time.Sleep(10 * time.Second)
	}
}


