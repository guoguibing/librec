dataset_name=song
input_path=${dataset_name}
iterator_maximum=100
model_splitter=testset
testset_path=${dataset_name}/test
eval_classes=mae,mse
output_name=default

ext_recommender_classes='slopeone'
factory_numbers='10 20 40 80'
regularizations='0.001 0.01 0.1 1.0'
for recommender_class in ${ext_recommender_classes}; do
  for factory_number in ${factory_numbers}; do
    for regularization in ${regularizations}; do
      output_name=${factory_number}_${regularization}
      # echo '  ' ${output_name}
      ./librec rec -exec \
        -D data.input.path=${input_path} \
        -D data.model.splitter=${model_splitter} \
        -D data.testset.path=${testset_path} \
        -D data.output.name=${output_name} \
        -D rec.eval.classes=${eval_classes} \
        -D rec.recommender.class=${recommender_class} \
        -D rec.iterator.maximum=200 \
        -D rec.factory.number=${factory_number} \
        -D rec.recommender.lambda.user=${regularization} \
        -D rec.recommender.lambda.item=${regularization}
    done
  done
done
exit 0

mf_recommender_classes='biasedmf bpmf nmf pmf rbm'
factor_numbers='10 50 100 500'
regularizations='0.001 0.01 0.1 1.0'
for recommender_class in ${mf_recommender_classes}; do
  # echo ${recommender_class}
  for factor_number in ${factor_numbers}; do
    for regularization in ${regularizations}; do
      output_name=${factor_number}_${regularization}
      # echo '  ' ${output_name}
      output_path=../result/${dataset_name}-${recommender_class}/${output_name}
      if [ -f ${output_path} ]
      then
        # echo 'find' ${output_path}
        continue
      fi
      echo 'not find' ${output_path}
      ./librec rec -exec \
        -D data.input.path=${input_path} \
        -D data.model.splitter=${model_splitter} \
        -D data.testset.path=${testset_path} \
        -D data.output.name=${output_name} \
        -D rec.eval.classes=${eval_classes} \
        -D rec.recommender.class=${recommender_class} \
        -D rec.factor.number=${factor_number} \
        -D rec.user.regularization=${regularization} \
        -D rec.item.regularization=${regularization}
    done
  done
done
